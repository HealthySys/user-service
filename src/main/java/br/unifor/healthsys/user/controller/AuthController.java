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
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    @Audited(action = "LOGIN", resource = "AUTH")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/register")
    @Audited(action = "REGISTER", resource = "USER")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping("/refresh")
    @Audited(action = "REFRESH_TOKEN", resource = "AUTH")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(userService.refresh(request));
    }

    @PostMapping("/logout")
    @Audited(action = "LOGOUT", resource = "AUTH")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        userService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bootstrap-status")
    public ResponseEntity<BootstrapStatusResponse> bootstrapStatus() {
        return ResponseEntity.ok(
                new BootstrapStatusResponse(userService.isBootstrapRequired(), userService.countUsers())
        );
    }
}
