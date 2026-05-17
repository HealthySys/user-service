package br.unifor.healthsys.user.dto;

import br.unifor.healthsys.user.model.User;
import br.unifor.healthsys.user.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String username;

    @NotBlank
    @Size(min = 3, max = 200)
    private String nome;

    @NotBlank
    @Email
    private String email;

    @ValidPassword(allowBlank = true)
    private String password;

    @NotNull
    private User.Role role;

    @NotNull
    private Boolean active;

    private String assinaturaDigital;
}
