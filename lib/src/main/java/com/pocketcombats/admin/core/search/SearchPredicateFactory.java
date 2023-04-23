package com.pocketcombats.admin.core.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Optional;

@FunctionalInterface
public interface SearchPredicateFactory {

    Optional<Predicate> build(CriteriaBuilder cb, Root<?> root, String searchQuery);
}
