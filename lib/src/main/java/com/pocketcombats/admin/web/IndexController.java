package com.pocketcombats.admin.web;

import com.pocketcombats.admin.conf.JpaAdminProperties;
import com.pocketcombats.admin.core.AdminModelRegistry;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.core.permission.AdminPermissionService;
import com.pocketcombats.admin.data.AdminModelInfo;
import com.pocketcombats.admin.data.AdminModelsGroup;
import com.pocketcombats.admin.history.AdminHistoryCompiler;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for the admin dashboard index page.
 * Displays a list of model groups and models that the current user has permission to view.
 */
@Controller
public class IndexController {

    private final JpaAdminProperties properties;
    private final AdminModelRegistry modelRegistry;
    private final AdminHistoryCompiler historyCompiler;
    private final AdminPermissionService permissionService;

    public IndexController(
            JpaAdminProperties properties,
            AdminModelRegistry modelRegistry,
            AdminHistoryCompiler historyCompiler,
            AdminPermissionService permissionService
    ) {
        this.properties = properties;
        this.modelRegistry = modelRegistry;
        this.historyCompiler = historyCompiler;
        this.permissionService = permissionService;
    }

    @RequestMapping("/admin/")
    @Secured("ROLE_JPA_ADMIN")
    public ModelAndView index() {
        return new ModelAndView(
                properties.getTemplates().index(),
                Map.of(
                        "modelGroups", getModelGroups(),
                        "historyEnabled", !properties.isDisableHistory(),
                        "history", historyCompiler.compileLog(properties.getHistorySize())
                )
        );
    }

    private List<AdminModelsGroup> getModelGroups() {
        var filteredModels = filterModelsByPermission(modelRegistry.getCategorizedModels());
        return collectModelGroups(filteredModels);
    }

    private Map<String, List<AdminRegisteredModel>> filterModelsByPermission(
            Map<String, List<AdminRegisteredModel>> models
    ) {
        return models.entrySet().stream()
                .map(entry -> Map.entry(
                        entry.getKey(),
                        entry.getValue().stream()
                                .filter(permissionService::canView)
                                .toList()
                ))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static List<AdminModelsGroup> collectModelGroups(Map<String, List<AdminRegisteredModel>> models) {
        return models.entrySet().stream()
                .map(entry -> new AdminModelsGroup(
                        entry.getKey(),
                        entry.getValue().stream()
                                .sorted(
                                        Comparator.comparingInt(AdminRegisteredModel::priority).reversed()
                                                .thenComparing(AdminRegisteredModel::modelName)
                                )
                                .map(model -> new AdminModelInfo(model.label(), model.modelName()))
                                .toList()
                ))
                .toList();
    }
}
