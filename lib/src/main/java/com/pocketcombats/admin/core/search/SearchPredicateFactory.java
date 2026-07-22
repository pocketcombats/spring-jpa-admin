package com.pocketcombats.admin.core.search;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;

import java.util.Optional;

@FunctionalInterface
public interface SearchPredicateFactory {

    /**
     * Builds a filter predicate for {@code from}, or empty when the query can't match this field.
     * <p>
     * {@code query} is the enclosing (sub)query; factories searching through collections use it
     * to spawn correlated {@code EXISTS} subqueries.
     * The returned predicate must never multiply result rows.
     */
    Optional<Predicate> build(CriteriaBuilder cb, AbstractQuery<?> query, From<?, ?> from, String searchQuery);
}
