package com.pocketcombats.admin.plugin.auth.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class ConfirmPasswordValidator implements ConstraintValidator<ConfirmPassword, ChangePasswordRequest> {

    @Override
    public boolean isValid(ChangePasswordRequest request, ConstraintValidatorContext context) {
        return StringUtils.trim(request.password()).equals(StringUtils.trim(request.confirmPassword()));
    }
}
