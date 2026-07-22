package com.pocketcombats.admin.core.search;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * Searches through a collection without multiplying result rows: correlates the searched entity
 * into an {@code EXISTS} subquery, joins {@code collectionPath} there, and delegates matching of
 * the remainder path to {@code delegate} against the joined element.
 */
public class ExistsSearchPredicateFactory implements SearchPredicateFactory {

    private final String collectionPath;
    private final SearchPredicateFactory delegate;

    public ExistsSearchPredicateFactory(String collectionPath, SearchPredicateFactory delegate) {
        this.collectionPath = collectionPath;
        this.delegate = delegate;
    }

    @Override
    public Optional<Predicate> build(CriteriaBuilder cb, AbstractQuery<?> query, From<?, ?> from, String searchQuery) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        From<?, ?> element = joinPath(correlate(subquery, from), collectionPath);
        return delegate.build(cb, subquery, element, searchQuery)
                .map(match -> {
                    subquery.select(cb.literal(1)).where(match);
                    return cb.exists(subquery);
                });
    }

    private static From<?, ?> correlate(Subquery<?> subquery, From<?, ?> from) {
        if (from instanceof Root<?> root) {
            return subquery.correlate(root);
        }
        if (from instanceof Join<?, ?> join) {
            return subquery.correlate(join);
        }
        throw new IllegalStateException("Unsupported From type: " + from.getClass().getName());
    }

    private static From<?, ?> joinPath(From<?, ?> from, String path) {
        // Inner joins are correct here: they only narrow the subquery, and an empty
        // subquery result is exactly "no matching element exists".
        From<?, ?> target = from;
        for (String part : StringUtils.split(path, '.')) {
            target = target.join(part);
        }
        return target;
    }
}
