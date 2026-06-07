package br.unifor.healthsys.user.controller;

import br.unifor.healthsys.user.audit.Audited;
import br.unifor.healthsys.user.dto.AuthRequest;
import br.unifor.healthsys.user.dto.AuthResponse;
import br.unifor.healthsys.user.dto.BootstrapStatusResponse;
import br.unifor.healthsys.user.dto.LogoutRequest;
import br.unifor.healthsys.user.dto.RefreshTokenRequest;
import br.unifor.healthsys.user.dto.UserRequest;
import br.unifor.healthsys.user.dto.UserResponse;
import br.unifor.healthsys.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login, registro e ciclo de vida dos tokens JWT")
@SecurityRequirements
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica um usuário", description = "Valida as credenciais e retorna o access token (JWT) e o refresh token.")
    @Audited(action = "LOGIN", resource = "AUTH")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Registra um novo usuário", description = "Cria um usuário. Usado no bootstrap inicial do sistema.")
    @Audited(action = "REGISTER", resource = "USER")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o access token", description = "Gera um novo access token a partir de um refresh token válido.")
    @Audited(action = "REFRESH_TOKEN", resource = "AUTH")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(userService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerra a sessão", description = "Invalida o refresh token informado.")
    @Audited(action = "LOGOUT", resource = "AUTH")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        userService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bootstrap-status")
    @Operation(summary = "Status de bootstrap", description = "Indica se o sistema ainda precisa do cadastro inicial de usuário e quantos usuários existem.")
    public ResponseEntity<BootstrapStatusResponse> bootstrapStatus() {
        return ResponseEntity.ok(
                new BootstrapStatusResponse(userService.isBootstrapRequired(), userService.countUsers())
        );
    }
}
