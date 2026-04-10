package br.unifor.healthsys.user.dto;

import br.unifor.healthsys.user.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String username;

    @NotBlank
    @Email
    private String email;

    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$",
            message = "A senha deve ter no mínimo 8 caracteres, com letras maiúsculas, minúsculas, números e símbolo."
    )
    private String password;

    @NotNull
    private User.Role role;

    @NotNull
    private Boolean active;
}
