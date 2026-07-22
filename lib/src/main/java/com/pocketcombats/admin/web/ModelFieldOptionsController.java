package com.pocketcombats.admin.web;

import com.pocketcombats.admin.core.AdminModelOptionsService;
import com.pocketcombats.admin.core.UnknownModelException;
import com.pocketcombats.admin.data.form.AdminSelectOptionsResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves paginated JSON options for to-one field autocomplete widgets.
 */
@RestController
public class ModelFieldOptionsController {

    private final AdminModelOptionsService optionsService;

    public ModelFieldOptionsController(AdminModelOptionsService optionsService) {
        this.optionsService = optionsService;
    }

    @GetMapping("/admin/{model}/field/{field}/options")
    @Secured("ROLE_JPA_ADMIN")
    public AdminSelectOptionsResponse options(
            @PathVariable String model,
            @PathVariable String field,
            @RequestParam(required = false) @Nullable String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) @Nullable String id
    ) throws UnknownModelException {
        if (id != null && !id.isEmpty()) {
            return optionsService.resolve(model, field, id);
        }
        return optionsService.options(model, field, q, page);
    }
}
