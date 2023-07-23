package com.pocketcombats.admin.core;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@FunctionalInterface
public interface PredicateFactory {

    Predicate create(Root<?> root);
}
