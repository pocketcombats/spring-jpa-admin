package com.pocketcombats.admin.core;

import com.pocketcombats.admin.core.search.SearchPredicateFactory;

import java.util.List;

public record AdminRegisteredModel(
        String modelName,
        String label,
        Class<?> entityClass,
        int pageSize,
        List<AdminModelListField> listFields,
        SearchPredicateFactory searchPredicateFactory,
        List<AdminModelFieldset> fieldsets
) {

}
