package com.pocketcombats.admin.core.search;

import com.pocketcombats.admin.util.AdminStringUtils;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;

import java.util.Locale;
import java.util.Optional;

public class TextSearchPredicateFactory implements SearchPredicateFactory {

    private final String path;

    public TextSearchPredicateFactory(String path) {
        this.path = path;
    }

    @Override
    public Optional<Predicate> build(CriteriaBuilder cb, AbstractQuery<?> query, From<?, ?> from, String searchQuery) {
        return Optional.of(
                cb.like(
                        cb.lower(PathUtils.resolve(from, path)),
                        // Locale.ROOT keeps the query side in sync with the database's LOWER()
                        // (the default locale would break e.g. under Turkish dotless-i rules)
                        "%" + AdminStringUtils.escapeLikeClause(searchQuery.toLowerCase(Locale.ROOT)) + "%",
                        '\\'
                )
        );
    }
}
