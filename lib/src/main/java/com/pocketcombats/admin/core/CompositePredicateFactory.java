package com.pocketcombats.admin.core;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Arrays;

public class CompositePredicateFactory implements PredicateFactory {

    private final PredicateFactory[] factories;
    private final CriteriaBuilder cb;

    public CompositePredicateFactory(EntityManager em, PredicateFactory[] factories) {
        this.factories = factories;
        this.cb = em.getCriteriaBuilder();
    }

    @Override
    public Predicate create(Root<?> root) {
        Predicate[] predicates = Arrays.stream(factories)
                .map(factory -> factory.create(root))
                .toArray(Predicate[]::new);
        return cb.and(predicates);
    }
}
