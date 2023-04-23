package com.pocketcombats.admin.core.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Optional;

public class CompositeSearchPredicateFactory implements SearchPredicateFactory {

    private final List<SearchPredicateFactory> factories;

    public CompositeSearchPredicateFactory(List<SearchPredicateFactory> factories) {
        this.factories = factories;
    }

    @Override
    public Optional<Predicate> build(CriteriaBuilder cb, Root<?> root, String searchQuery) {
        return Optional.of(
                cb.or(
                        factories.stream()
                                .flatMap(factory -> factory.build(cb, root, searchQuery).stream())
                                .toArray(Predicate[]::new)
                )
        );
    }
}
