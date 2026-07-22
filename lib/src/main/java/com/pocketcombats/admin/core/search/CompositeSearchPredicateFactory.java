package com.pocketcombats.admin.core.search;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;

import java.util.List;
import java.util.Optional;

public class CompositeSearchPredicateFactory implements SearchPredicateFactory {

    private final List<SearchPredicateFactory> factories;

    public CompositeSearchPredicateFactory(List<SearchPredicateFactory> factories) {
        this.factories = factories;
    }

    @Override
    public Optional<Predicate> build(CriteriaBuilder cb, AbstractQuery<?> query, From<?, ?> from, String searchQuery) {
        List<Predicate> predicates = factories.stream()
                .flatMap(factory -> factory.build(cb, query, from, searchQuery).stream())
                .toList();
        return switch (predicates.size()) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of(predicates.get(0));
            default -> Optional.of(cb.or(predicates.toArray(new Predicate[0])));
        };
    }
}
