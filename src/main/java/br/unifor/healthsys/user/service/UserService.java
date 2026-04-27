package br.unifor.healthsys.user.service;

import br.unifor.healthsys.user.dto.AuthRequest;
import br.unifor.healthsys.user.dto.AuthResponse;
import br.unifor.healthsys.user.dto.LogoutRequest;
import br.unifor.healthsys.user.dto.RefreshTokenRequest;
import br.unifor.healthsys.user.dto.UserRequest;
import br.unifor.healthsys.user.dto.UserResponse;
import br.unifor.healthsys.user.dto.UserUpdateRequest;
import br.unifor.healthsys.user.exception.ConflictException;
import br.unifor.healthsys.user.exception.NotFoundException;
import br.unifor.healthsys.user.model.RefreshToken;
import br.unifor.healthsys.user.model.User;
import br.unifor.healthsys.user.repository.RefreshTokenRepository;
import br.unifor.healthsys.user.repository.UserRepository;
import br.unifor.healthsys.user.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = findByUsernameOrEmail(request.getUsername())
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));

        return buildAuthResponse(user, issueRefreshToken(user));
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new NotFoundException("Refresh token nao encontrado."));

        if (!token.isAvailable()) {
            throw new IllegalArgumentException("Refresh token invalido, expirado ou ja utilizado.");
        }

        token.markUsed();
        refreshTokenRepository.save(token);

        return buildAuthResponse(token.getUser(), issueRefreshToken(token.getUser()));
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken()).ifPresent(existing -> {
            existing.revoke();
            refreshTokenRepository.save(existing);
        });
    }

    public UserResponse register(UserRequest request) {
        validateUniqueFields(request.getUsername(), request.getEmail(), null);
        return saveNewUser(request);
    }

    public UserResponse create(UserRequest request) {
        validateUniqueFields(request.getUsername(), request.getEmail(), null);
        return saveNewUser(request);
    }

    public boolean isBootstrapRequired() {
        return userRepository.count() == 0;
    }

    public long countUsers() {
        return userRepository.count();
    }

    public UserResponse findCurrentUser(String identifier) {
        return findByUsernameOrEmail(identifier)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("Usuário autenticado não encontrado."));
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + id));
    }

    public UserResponse update(Long id, UserUpdateRequest request) {
        User existing = findUserEntity(id);
        validateUniqueFields(request.getUsername(), request.getEmail(), id);

        existing.setUsername(request.getUsername());
        existing.setNome(request.getNome());
        existing.setEmail(request.getEmail());
        existing.setRole(request.getRole());
        existing.setActive(Boolean.TRUE.equals(request.getActive()));
        existing.setAssinaturaDigital(request.getAssinaturaDigital());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return UserResponse.from(userRepository.save(existing));
    }

    public UserResponse updateStatus(Long id, boolean active) {
        User existing = findUserEntity(id);
        existing.setActive(active);
        return UserResponse.from(userRepository.save(existing));
    }

    public void delete(Long id) {
        userRepository.delete(findUserEntity(id));
    }

    private UserResponse saveNewUser(UserRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .nome(request.getNome())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .assinaturaDigital(request.getAssinaturaDigital())
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    private Optional<User> findByUsernameOrEmail(String identifier) {
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier));
    }

    private User findUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + id));
    }

    private void validateUniqueFields(String username, String email, Long currentUserId) {
        userRepository.findByUsername(username)
                .filter(user -> !Objects.equals(user.getId(), currentUserId))
                .ifPresent(user -> {
                    throw new ConflictException("Nome de usuário já existe: " + username);
                });

        userRepository.findByEmail(email)
                .filter(user -> !Objects.equals(user.getId(), currentUserId))
                .ifPresent(user -> {
                    throw new ConflictException("E-mail já cadastrado: " + email);
                });
    }

    private RefreshToken issueRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private AuthResponse buildAuthResponse(User user, RefreshToken refreshToken) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .refreshToken(refreshToken.getToken())
                .refreshExpiresIn(jwtService.getRefreshExpirationMs() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .nome(user.getNome())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
