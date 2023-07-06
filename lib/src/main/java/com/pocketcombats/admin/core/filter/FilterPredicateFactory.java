package com.pocketcombats.admin.core.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@FunctionalInterface
public interface FilterPredicateFactory {

    Predicate createPredicate(CriteriaBuilder cb, Root<?> root, String value);
}
