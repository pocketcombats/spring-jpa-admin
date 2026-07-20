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
        List<Predicate> predicates = factories.stream()
                .flatMap(factory -> factory.build(cb, root, searchQuery).stream())
                .toList();
        return switch (predicates.size()) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of(predicates.get(0));
            default -> Optional.of(cb.or(predicates.toArray(new Predicate[0])));
        };
    }
}
