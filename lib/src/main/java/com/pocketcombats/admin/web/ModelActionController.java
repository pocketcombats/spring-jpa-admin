package com.pocketcombats.admin.web;

import com.pocketcombats.admin.conf.JpaAdminProperties;
import com.pocketcombats.admin.core.UnknownModelException;
import com.pocketcombats.admin.core.action.AdminModelActionService;
import com.pocketcombats.admin.core.action.UnknownActionException;
import com.pocketcombats.admin.data.action.ActionPrompt;
import com.pocketcombats.admin.data.action.ActionRequest;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

@Controller
public class ModelActionController {

    private final JpaAdminProperties properties;
    private final AdminModelActionService service;

    public ModelActionController(JpaAdminProperties properties, AdminModelActionService service) {
        this.properties = properties;
        this.service = service;
    }

    @PostMapping(path = "/admin/{model}/edit/{id}/", params = "delete")
    @Secured("ROLE_JPA_ADMIN")
    public ModelAndView update(
            @PathVariable("model") String model,
            @PathVariable("id") String id
    ) throws UnknownModelException, UnknownActionException {
        return actionPrompt(model, new ActionRequest("delete", List.of(id)));
    }

    @PostMapping("/admin/{model}/action/")
    @Secured("ROLE_JPA_ADMIN")
    public ModelAndView actionPrompt(
            @PathVariable("model") String model,
            ActionRequest request
    ) throws UnknownModelException, UnknownActionException {
        if (request.getId() == null || request.getId().isEmpty()) {
            return new ModelAndView("redirect:/admin/" + model + "/");
        }

        ActionPrompt prompt = service.prompt(model, request.getAction(), request.getId());
        return new ModelAndView(
                properties.getTemplates().actionPrompt(),
                Map.of("prompt", prompt)
        );
    }

    @PostMapping("/admin/{model}/action/confirm/")
    @Secured("ROLE_JPA_ADMIN")
    public String confirmAction(
            @PathVariable("model") String model,
            ActionRequest confirmation
    ) throws UnknownModelException, UnknownActionException {
        if (confirmation.getId() != null && !confirmation.getId().isEmpty()) {
            service.perform(model, confirmation.getAction(), confirmation.getId());
        }
        return "redirect:/admin/" + model + "/";
    }
}
