package com.pocketcombats.admin.core.search;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;

import java.util.Optional;
import java.util.UUID;

public class UUIDSearchPredicateFactory implements SearchPredicateFactory {

    private final String path;

    public UUIDSearchPredicateFactory(String path) {
        this.path = path;
    }

    @Override
    public Optional<Predicate> build(CriteriaBuilder cb, AbstractQuery<?> query, From<?, ?> from, String searchQuery) {
        UUID uuid;
        try {
            uuid = UUID.fromString(searchQuery);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return Optional.of(cb.equal(PathUtils.resolve(from, path), uuid));
    }
}
