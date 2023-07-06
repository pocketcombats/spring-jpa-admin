package com.pocketcombats.admin.core.filter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;

import java.util.Collections;
import java.util.List;

public class BooleanFilterOptionsCollector extends AbstractAttributeFilterOptionsCollector {

    public BooleanFilterOptionsCollector(
            EntityManager em,
            EntityType<?> entityType,
            Attribute<?, ?> attribute
    ) {
        super(em, entityType, attribute);
    }

    @Override
    protected List<ModelFilterOption> mapResults(List<?> resultList) {
        if (resultList.size() > 1) {
            return List.of(
                    new ModelFilterOption("spring-jpa-admin.filter.boolean.true", "true", true),
                    new ModelFilterOption("spring-jpa-admin.filter.boolean.false", "false", true)
            );
        } else {
            return Collections.emptyList();
        }
    }
}
