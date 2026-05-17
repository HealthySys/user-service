package br.unifor.healthsys.user.dto;

import br.unifor.healthsys.user.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String nome;
    private String email;
    private String role;
    private String assinaturaDigital;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nome(user.getNome())
                .email(user.getEmail())
                .role(user.getRole().name())
                .assinaturaDigital(user.getAssinaturaDigital())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
