package com.pocketcombats.admin.plugin.auth.web;

import jakarta.validation.constraints.NotBlank;

@ConfirmPassword
public record ChangePasswordRequest(
        @NotBlank String password,
        String confirmPassword
) {
}
