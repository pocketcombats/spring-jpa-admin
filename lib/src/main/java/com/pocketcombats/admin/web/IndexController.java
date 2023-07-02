package com.pocketcombats.admin.web;

import com.pocketcombats.admin.conf.JpaAdminProperties;
import com.pocketcombats.admin.core.AdminModelRegistry;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
public class IndexController {

    private final JpaAdminProperties properties;
    private final AdminModelRegistry entityRegistry;

    public IndexController(JpaAdminProperties properties, AdminModelRegistry entityRegistry) {
        this.properties = properties;
        this.entityRegistry = entityRegistry;
    }

    @RequestMapping("/admin/")
    @Secured("ROLE_JPA_ADMIN")
    public ModelAndView index() {
        return new ModelAndView(
                properties.getTemplates().getIndex(),
                Map.of("models", entityRegistry.listModels())
        );
    }
}
