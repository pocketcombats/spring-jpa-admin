package com.pocketcombats.admin.core;

import com.pocketcombats.admin.core.action.AdminModelAction;
import com.pocketcombats.admin.core.filter.AdminModelFilter;
import com.pocketcombats.admin.core.links.AdminModelLink;
import com.pocketcombats.admin.core.search.SearchPredicateFactory;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;

public record AdminRegisteredModel(
        String modelName,
        int priority,
        String label,
        RegisteredEntityDetails entityDetails,
        boolean insertable,
        boolean updatable,
        int pageSize,
        List<AdminModelListField> listFields,
        @Nullable String defaultOrder,
        @Nullable SearchPredicateFactory searchPredicateFactory,
        List<AdminModelFilter> filters,
        List<AdminModelFieldset> fieldsets,
        List<AdminModelLink> links,
        Map<String, AdminModelAction> actions
) {

}
