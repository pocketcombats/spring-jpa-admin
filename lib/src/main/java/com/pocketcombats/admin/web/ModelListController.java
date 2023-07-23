package com.pocketcombats.admin.web;

import com.pocketcombats.admin.conf.JpaAdminProperties;
import com.pocketcombats.admin.core.AdminModelEntitiesListService;
import com.pocketcombats.admin.core.UnknownModelException;
import com.pocketcombats.admin.core.links.AdminRelationLinkService;
import com.pocketcombats.admin.data.list.EntityRelation;
import com.pocketcombats.admin.data.list.ModelRequest;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ModelListController {

    private final JpaAdminProperties properties;
    private final AdminModelEntitiesListService entitiesListService;
    private final AdminRelationLinkService relationLinkService;

    public ModelListController(
            JpaAdminProperties properties,
            AdminModelEntitiesListService entitiesListService,
            AdminRelationLinkService relationLinkService
    ) {
        this.properties = properties;
        this.entitiesListService = entitiesListService;
        this.relationLinkService = relationLinkService;
    }

    @GetMapping("/admin/{model}/")
    @Secured("ROLE_JPA_ADMIN")
    public ModelAndView list(
            @PathVariable String model,
            ModelRequest modelRequest,
            @RequestParam Map<String, String> data
    ) throws UnknownModelException {
        Map<String, String> filters = collectFilters(data);
        return new ModelAndView(
                properties.getTemplates().getList(),
                Map.of(
                        "entities", entitiesListService.listEntities(model, modelRequest, filters),
                        "query", modelRequest
                )
        );
    }

    @GetMapping("/admin/{model}/{id}/rel/{relation}/")
    @Secured("ROLE_JPA_ADMIN")
    public ModelAndView relationList(
            @PathVariable String model,
            @PathVariable String id,
            @PathVariable String relation,
            ModelRequest modelRequest,
            @RequestParam Map<String, String> data
    ) throws UnknownModelException {
        Map<String, String> filters = collectFilters(data);
        EntityRelation entityRelation = new EntityRelation(model, id);
        return new ModelAndView(
                properties.getTemplates().getList(),
                Map.of(
                        "entities", entitiesListService.listRelationEntities(
                                relation,
                                modelRequest,
                                filters,
                                entityRelation
                        ),
                        "parent", relationLinkService.getParentInfo(entityRelation),
                        "query", modelRequest
                )
        );
    }

    private static Map<String, String> collectFilters(Map<String, String> requestData) {
        return requestData.entrySet().stream()
                .filter(e -> e.getKey().startsWith("filter:"))
                .collect(Collectors.toMap(
                        e -> e.getKey().substring(7),
                        Map.Entry::getValue
                ));
    }
}
