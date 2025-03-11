package com.pocketcombats.admin.core.uniqueness;

import com.pocketcombats.admin.core.predicate.ValuePredicateFactory;
import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SingleAdminUniqueConstraint implements AdminUniqueConstraint {

    private final String label;
    private final ValuePredicateFactory predicateFactory;
    private final AdminModelPropertyReader propertyReader;

    public SingleAdminUniqueConstraint(
            String label,
            ValuePredicateFactory predicateFactory,
            AdminModelPropertyReader propertyReader
    ) {
        this.label = label;
        this.predicateFactory = predicateFactory;
        this.propertyReader = propertyReader;
    }

    @Override
    public Predicate createPredicate(CriteriaBuilder cb, Root<?> root, Object entity) {
        return predicateFactory.createPredicate(cb, root, propertyReader.getValue(entity));
    }

    @Override
    public boolean matches(Object entityA, Object entityB) {
        return Objects.equals(propertyReader.getValue(entityA), propertyReader.getValue(entityB));
    }

    @Override
    public List<String> getFieldLabels() {
        return Collections.singletonList(label);
    }
}
