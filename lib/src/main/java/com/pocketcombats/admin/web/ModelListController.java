package com.pocketcombats.admin.web;

import com.pocketcombats.admin.conf.JpaAdminProperties;
import com.pocketcombats.admin.core.AdminModelEntitiesListService;
import com.pocketcombats.admin.core.UnknownModelException;
import com.pocketcombats.admin.data.list.ModelRequest;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
public class ModelListController {

    private final JpaAdminProperties properties;
    private final AdminModelEntitiesListService adminModelEntitiesListService;

    public ModelListController(JpaAdminProperties properties, AdminModelEntitiesListService adminModelEntitiesListService) {
        this.properties = properties;
        this.adminModelEntitiesListService = adminModelEntitiesListService;
    }

    @GetMapping("/admin/{model}/")
    @Secured("ROLE_JPA_ADMIN")
    public ModelAndView list(@PathVariable String model, ModelRequest modelRequest) throws UnknownModelException {
        return new ModelAndView(
                properties.getTemplates().getList(),
                Map.of(
                        "entities", adminModelEntitiesListService.listEntities(model, modelRequest),
                        "query", modelRequest
                )
        );
    }
}
