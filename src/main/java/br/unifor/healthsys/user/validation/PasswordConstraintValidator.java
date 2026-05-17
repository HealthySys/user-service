package br.unifor.healthsys.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$";

    private boolean allowBlank;

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        allowBlank = constraintAnnotation.allowBlank();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return allowBlank;
        }
        return value.matches(PASSWORD_REGEX);
    }
}
