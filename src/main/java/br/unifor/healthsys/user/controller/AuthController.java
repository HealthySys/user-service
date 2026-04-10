package br.unifor.healthsys.user.controller;

import br.unifor.healthsys.user.dto.AuthRequest;
import br.unifor.healthsys.user.dto.AuthResponse;
import br.unifor.healthsys.user.dto.BootstrapStatusResponse;
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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @GetMapping("/bootstrap-status")
    public ResponseEntity<BootstrapStatusResponse> bootstrapStatus() {
        return ResponseEntity.ok(
                new BootstrapStatusResponse(userService.isBootstrapRequired(), userService.countUsers())
        );
    }
}
