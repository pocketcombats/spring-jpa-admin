package com.pocketcombats.admin.core.predicate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface ValuePredicateFactory {

    Predicate createPredicate(CriteriaBuilder cb, Root<?> root, @Nullable Object value);
}
