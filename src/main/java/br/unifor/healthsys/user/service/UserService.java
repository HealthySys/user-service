package br.unifor.healthsys.user.service;

import br.unifor.healthsys.user.dto.AuthRequest;
import br.unifor.healthsys.user.dto.AuthResponse;
import br.unifor.healthsys.user.dto.UserRequest;
import br.unifor.healthsys.user.dto.UserResponse;
import br.unifor.healthsys.user.dto.UserUpdateRequest;
import br.unifor.healthsys.user.exception.ConflictException;
import br.unifor.healthsys.user.exception.NotFoundException;
import br.unifor.healthsys.user.model.User;
import br.unifor.healthsys.user.repository.UserRepository;
import br.unifor.healthsys.user.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = findByUsernameOrEmail(request.getUsername())
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
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

    public UserResponse update(Long id, UserUpdateRequest request) {
        User existing = findUserEntity(id);
        validateUniqueFields(request.getUsername(), request.getEmail(), id);

        existing.setUsername(request.getUsername());
        existing.setEmail(request.getEmail());
        existing.setRole(request.getRole());
        existing.setActive(Boolean.TRUE.equals(request.getActive()));

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
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .build();

        return UserResponse.from(userRepository.save(user));
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
}
