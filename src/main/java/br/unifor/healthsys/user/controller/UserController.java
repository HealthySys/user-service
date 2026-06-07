package br.unifor.healthsys.user.controller;

import br.unifor.healthsys.user.audit.Audited;
import br.unifor.healthsys.user.dto.UserRequest;
import br.unifor.healthsys.user.dto.UserResponse;
import br.unifor.healthsys.user.dto.UserStatusRequest;
import br.unifor.healthsys.user.dto.UserUpdateRequest;
import br.unifor.healthsys.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuários", description = "Gestão de usuários e perfis (acesso restrito a ADMIN, exceto /me)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado", description = "Retorna o perfil do usuário dono do token informado.")
    public ResponseEntity<UserResponse> findCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.findCurrentUser(authentication.getName()));
    }

    @GetMapping
    @Operation(summary = "Lista todos os usuários", description = "Somente ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "READ_ALL", resource = "USER")
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca usuário por ID", description = "Somente ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "READ", resource = "USER")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Cria um usuário", description = "Somente ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "CREATE", resource = "USER")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um usuário", description = "Somente ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "UPDATE", resource = "USER")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ativa/inativa um usuário", description = "Somente ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "UPDATE_STATUS", resource = "USER")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable Long id,
                                                     @Valid @RequestBody UserStatusRequest request) {
        return ResponseEntity.ok(userService.updateStatus(id, request.getActive()));
    }
}
