package com.pocketcombats.admin.core.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Optional;
import java.util.UUID;

public class UUIDSearchPredicateFactory implements SearchPredicateFactory{

    private final String path;

    public UUIDSearchPredicateFactory(String path) {
        this.path = path;
    }

    @Override
    public Optional<Predicate> build(CriteriaBuilder cb, Root<?> root, String searchQuery) {
        UUID uuid;
        try {
            uuid = UUID.fromString(searchQuery);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return Optional.of(cb.equal(PathUtils.resolve(root, path), uuid));
    }
}
