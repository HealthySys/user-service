package br.unifor.healthsys.user.security;

import br.unifor.healthsys.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.InvalidKeyException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.private-key-location}")
    private Resource privateKeyResource;

    @Value("${jwt.public-key-location}")
    private Resource publicKeyResource;

    @Value("${jwt.key-id}")
    private String keyId;

    @Value("${jwt.expiration-ms:3600000}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration-ms:86400000}")
    private long refreshExpirationMs;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @PostConstruct
    void initKeys() {
        try {
            privateKey = (RSAPrivateKey) readPrivateKey(privateKeyResource);
            publicKey = (RSAPublicKey) readPublicKey(publicKeyResource);
        } catch (IOException | GeneralSecurityException e) {
            throw new InvalidKeyException("Falha ao carregar chaves RSA do JWT.", e);
        }
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .header()
                .keyId(keyId)
                .and()
                .subject(user.getUsername())
                .claims(Map.of(
                        "role", user.getRole().name(),
                        "email", user.getEmail(),
                        "nome", user.getNome(),
                        "userId", user.getId()
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    @Cacheable("jwt-validation")
    public CachedTokenDetails validateToken(String token) {
        Claims claims = extractClaims(token);
        Object rawUserId = claims.get("userId");
        Long userId = rawUserId instanceof Number number ? number.longValue() : null;

        return new CachedTokenDetails(
                claims.getSubject(),
                claims.get("role", String.class),
                claims.get("email", String.class),
                claims.get("nome", String.class),
                userId,
                claims.getExpiration().toInstant()
        );
    }

    public String extractUsername(String token) {
        return validateToken(token).username();
    }

    public boolean isTokenValid(String token) {
        validateToken(token);
        return true;
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public Map<String, Object> getJwks() {
        return Map.of(
                "keys", List.of(Map.of(
                        "kty", "RSA",
                        "kid", keyId,
                        "use", "sig",
                        "alg", "RS256",
                        "n", base64Url(publicKey.getModulus()),
                        "e", base64Url(publicKey.getPublicExponent())
                ))
        );
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private PrivateKey readPrivateKey(Resource resource) throws IOException, GeneralSecurityException {
        String pem = resource.getContentAsString(StandardCharsets.UTF_8);
        String content = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Decoders.BASE64.decode(content);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private PublicKey readPublicKey(Resource resource) throws IOException, GeneralSecurityException {
        String pem = resource.getContentAsString(StandardCharsets.UTF_8);
        String content = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Decoders.BASE64.decode(content);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private String base64Url(BigInteger value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(stripLeadingZero(value.toByteArray()));
    }

    private byte[] stripLeadingZero(byte[] bytes) {
        if (bytes.length > 1 && bytes[0] == 0) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
    }
}
