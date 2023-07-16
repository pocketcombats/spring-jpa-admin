package com.pocketcombats.admin.web;

import com.pocketcombats.admin.conf.JpaAdminProperties;
import com.pocketcombats.admin.core.AdminModelRegistry;
import com.pocketcombats.admin.history.AdminHistoryCompiler;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
public class IndexController {

    private final JpaAdminProperties properties;
    private final AdminModelRegistry entityRegistry;
    private final AdminHistoryCompiler historyCompiler;

    public IndexController(
            JpaAdminProperties properties,
            AdminModelRegistry entityRegistry,
            AdminHistoryCompiler historyCompiler
    ) {
        this.properties = properties;
        this.entityRegistry = entityRegistry;
        this.historyCompiler = historyCompiler;
    }

    @RequestMapping("/admin/")
    @Secured("ROLE_JPA_ADMIN")
    public ModelAndView index() {
        return new ModelAndView(
                properties.getTemplates().getIndex(),
                Map.of(
                        "modelGroups", entityRegistry.getModelGroups(),
                        "historyEnabled", !properties.isDisableHistory(),
                        "history", historyCompiler.compileLog(properties.getHistorySize())
                )
        );
    }
}
