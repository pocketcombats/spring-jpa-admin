package com.pocketcombats.admin.core.uniqueness;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.stream.Collectors;

public class CompositeAdminUniqueConstraint implements AdminUniqueConstraint {

    private final List<? extends AdminUniqueConstraint> constraints;

    public CompositeAdminUniqueConstraint(List<? extends AdminUniqueConstraint> constraints) {
        this.constraints = constraints;
    }

    @Override
    public Predicate createPredicate(CriteriaBuilder cb, Root<?> root, Object entity) {
        var predicates = constraints.stream()
                .map(constraint -> constraint.createPredicate(cb, root, entity))
                .toArray(Predicate[]::new);
        return cb.and(predicates);
    }

    @Override
    public boolean matches(Object entityA, Object entityB) {
        return constraints.stream()
                .allMatch(constraint -> constraint.matches(entityA, entityB));
    }

    @Override
    public List<String> getFieldLabels() {
        return constraints.stream()
                .flatMap(constraint -> constraint.getFieldLabels().stream())
                .collect(Collectors.toList());
    }
}
