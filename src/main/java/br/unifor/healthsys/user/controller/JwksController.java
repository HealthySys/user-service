package br.unifor.healthsys.user.controller;

import br.unifor.healthsys.user.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "JWKS", description = "Chaves públicas para validação dos tokens JWT pelos demais serviços")
@SecurityRequirements
public class JwksController {

    private final JwtService jwtService;

    public JwksController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "JSON Web Key Set", description = "Expõe as chaves públicas (formato JWKS) usadas para validar a assinatura dos tokens.")
    public ResponseEntity<Map<String, Object>> getJwks() {
        return ResponseEntity.ok(jwtService.getJwks());
    }
}
