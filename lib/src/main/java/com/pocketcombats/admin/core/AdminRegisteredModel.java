package com.pocketcombats.admin.core;

import com.pocketcombats.admin.core.filter.AdminModelFilter;
import com.pocketcombats.admin.core.search.SearchPredicateFactory;
import jakarta.annotation.Nullable;

import java.util.List;

public record AdminRegisteredModel(
        String modelName,
        String label,
        Class<?> entityClass,
        int pageSize,
        List<AdminModelListField> listFields,
        @Nullable SearchPredicateFactory searchPredicateFactory,
        List<AdminModelFilter> filters,
        List<AdminModelFieldset> fieldsets
) {

}
