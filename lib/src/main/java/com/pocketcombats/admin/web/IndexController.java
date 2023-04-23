package com.pocketcombats.admin.web;

import com.pocketcombats.admin.core.AdminModelRegistry;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
public class IndexController {

    private final AdminModelRegistry entityRegistry;

    public IndexController(AdminModelRegistry entityRegistry) {
        this.entityRegistry = entityRegistry;
    }

    @RequestMapping("/admin/")
    @Secured("ROLE_JPA_ADMIN")
    public ModelAndView index() {
        return new ModelAndView(
                "admin/index",
                Map.of("models", entityRegistry.listModels())
        );
    }
}
