package br.unifor.healthsys.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private String refreshToken;
    private Long refreshExpiresIn;
    private Long userId;
    private String username;
    private String nome;
    private String email;
    private String role;
}
