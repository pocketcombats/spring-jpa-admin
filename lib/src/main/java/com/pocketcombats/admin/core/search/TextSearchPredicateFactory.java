package com.pocketcombats.admin.core.search;

import com.pocketcombats.admin.util.AdminStringUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Optional;

public class TextSearchPredicateFactory implements SearchPredicateFactory {

    private final String path;

    public TextSearchPredicateFactory(String path) {
        this.path = path;
    }

    @Override
    public Optional<Predicate> build(CriteriaBuilder cb, Root<?> root, String searchQuery) {
        return Optional.of(
                cb.like(
                        cb.lower(PathUtils.resolve(root, path)),
                        "%" + AdminStringUtils.escapeLikeClause(searchQuery.toLowerCase()) + "%",
                        '\\'
                )
        );
    }
}
