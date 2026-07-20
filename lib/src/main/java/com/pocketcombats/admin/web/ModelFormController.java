package com.pocketcombats.admin.web;

import com.pocketcombats.admin.conf.JpaAdminProperties;
import com.pocketcombats.admin.core.AdminModelEditingResult;
import com.pocketcombats.admin.core.AdminModelFormService;
import com.pocketcombats.admin.core.UnknownModelException;
import com.pocketcombats.admin.data.form.EntityDetails;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.Objects;

@Controller
public class ModelFormController {

    private final JpaAdminProperties properties;
    private final AdminModelFormService adminModelFormService;

    public ModelFormController(JpaAdminProperties properties, AdminModelFormService adminModelFormService) {
        this.properties = properties;
        this.adminModelFormService = adminModelFormService;
    }

    @GetMapping("/admin/{modelName}/create/")
    @Secured("ROLE_JPA_ADMIN")
    public ModelAndView create(@PathVariable String modelName) throws UnknownModelException {
        EntityDetails entity = adminModelFormService.create(modelName);
        return new ModelAndView(
                properties.getTemplates().form(),
                Map.of("entity", entity)
        );
    }

    @PostMapping("/admin/{model}/create/")
    @Secured("ROLE_JPA_ADMIN")
    public ModelAndView create(
            @PathVariable String model,
            @RequestParam MultiValueMap<String, String> data
    ) throws UnknownModelException {
        AdminModelEditingResult result = adminModelFormService.create(model, data);
        if (result.bindingResult().hasErrors()) {
            return new ModelAndView(
                    properties.getTemplates().form(),
                    Map.of(
                            "entity", result.entityDetails(),
                            "errors", result.bindingResult()
                    )
            );
        } else {
            if (data.containsKey("save-continue")) {
                // A URI template, not concatenation: RedirectView percent-encodes each expanded
                // variable, which string concatenation would skip — breaking string ids containing
                // reserved characters. {model} fills from the current request's path variable; the
                // new entity's id is not a request variable and is supplied through the model.
                return new ModelAndView(
                        "redirect:/admin/{model}/edit/{id}/",
                        Map.of("id", Objects.requireNonNull(result.entityDetails().id()))
                );
            } else {
                return new ModelAndView("redirect:/admin/{model}/");
            }
        }
    }

    @GetMapping("/admin/{modelName}/edit/{id}/")
    @Secured("ROLE_JPA_ADMIN")
    public ModelAndView view(@PathVariable String modelName, @PathVariable String id) throws UnknownModelException {
        EntityDetails entity = adminModelFormService.details(modelName, id);
        return new ModelAndView(
                properties.getTemplates().form(),
                Map.of("entity", entity)
        );
    }

    @PostMapping("/admin/{model}/edit/{id}/")
    @Secured("ROLE_JPA_ADMIN")
    public ModelAndView update(
            @PathVariable String model,
            @PathVariable String id,
            @RequestParam MultiValueMap<String, String> data
    ) throws UnknownModelException {
        AdminModelEditingResult result = adminModelFormService.update(model, id, data);
        if (result.bindingResult().hasErrors()) {
            return new ModelAndView(
                    properties.getTemplates().form(),
                    Map.of(
                            "entity", result.entityDetails(),
                            "errors", result.bindingResult()
                    )
            );
        } else {
            if (data.containsKey("save-continue")) {
                // URI template so RedirectView re-encodes {model} and {id} from the request's path
                // variables; concatenating the decoded values would corrupt ids with reserved characters
                return new ModelAndView("redirect:/admin/{model}/edit/{id}/");
            } else {
                return new ModelAndView("redirect:/admin/{model}/");
            }
        }
    }
}
