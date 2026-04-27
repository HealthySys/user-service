package br.unifor.healthsys.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordConstraintValidator.class)
public @interface ValidPassword {

    String message() default "A senha deve ter no mínimo 8 caracteres, com letras maiúsculas, minúsculas, números e símbolo.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    boolean allowBlank() default false;
}
