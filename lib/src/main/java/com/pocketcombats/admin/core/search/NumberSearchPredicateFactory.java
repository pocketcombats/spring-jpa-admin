package com.pocketcombats.admin.core.search;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;

import java.util.Optional;

public class NumberSearchPredicateFactory implements SearchPredicateFactory {

    private final String path;
    private final Class<? extends Number> type;
    private final ConversionService conversionService;

    public NumberSearchPredicateFactory(String path, Class<? extends Number> type, ConversionService conversionService) {
        this.path = path;
        this.type = type;
        this.conversionService = conversionService;
    }

    @Override
    public Optional<Predicate> build(CriteriaBuilder cb, AbstractQuery<?> query, From<?, ?> from, String searchQuery) {
        Number value;
        try {
            value = conversionService.convert(searchQuery, type);
        } catch (ConversionException e) {
            return Optional.empty();
        }

        return Optional.of(cb.equal(PathUtils.resolve(from, path), value));
    }
}
