package com.pocketcombats.admin.plugin.auth.web;

import com.pocketcombats.admin.core.AdminModelFormService;
import com.pocketcombats.admin.core.UnknownModelException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
public class ChangePasswordController {

    private final AdminModelFormService adminModelFormService;
    private final PasswordEncoder passwordEncoder;

    public ChangePasswordController(AdminModelFormService adminModelFormService, PasswordEncoder passwordEncoder) {
        this.adminModelFormService = adminModelFormService;
        this.passwordEncoder = passwordEncoder;
    }

    @Secured("ROLE_JPA_SUPERADMIN")
    @GetMapping("/admin/{modelName}/{entityId}/change-{fieldName}")
    public ModelAndView changePassword(
            @PathVariable String modelName, @PathVariable String entityId, @PathVariable String fieldName
    ) throws UnknownModelException {
        return new ModelAndView(
                "admin/change-password",
                Map.of(
                        "entity", adminModelFormService.details(modelName, entityId),
                        "fieldName", fieldName
                )
        );
    }

    @Secured("ROLE_JPA_SUPERADMIN")
    @PostMapping("/admin/{modelName}/{entityId}/change-{fieldName}")
    public ModelAndView changePassword(
            @PathVariable String modelName, @PathVariable String entityId, @PathVariable String fieldName,
            @Validated ChangePasswordRequest changePasswordRequest, BindingResult errors
    ) throws UnknownModelException {
        if (errors.hasErrors()) {
            return new ModelAndView(
                    "admin/change-password",
                    Map.of(
                            "errors", errors,
                            "entity", adminModelFormService.details(modelName, entityId),
                            "fieldName", fieldName
                    )
            );
        }
        return doChangePassword(modelName, entityId, fieldName, changePasswordRequest);
    }

    private ModelAndView doChangePassword(
            String modelName, String entityId, String fieldName,
            ChangePasswordRequest changePasswordRequest
    ) throws UnknownModelException {
        String newPassword = passwordEncoder.encode(StringUtils.trim(changePasswordRequest.password()));
        BindingResult bindingResult = adminModelFormService.updateField(modelName, entityId, fieldName, newPassword);
        if (bindingResult.hasErrors()) {
            return new ModelAndView(
                    "admin/change-password",
                    Map.of(
                            "errors", bindingResult,
                            "entity", adminModelFormService.details(modelName, entityId),
                            "fieldName", fieldName
                    )
            );
        } else {
            return new ModelAndView("redirect:/admin/{modelName}/edit/{entityId}/");
        }
    }
}
