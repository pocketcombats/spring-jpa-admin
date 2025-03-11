package com.pocketcombats.admin.core.predicate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@FunctionalInterface
public interface ValuePredicateFactory {

    Predicate createPredicate(CriteriaBuilder cb, Root<?> root, Object value);
}
