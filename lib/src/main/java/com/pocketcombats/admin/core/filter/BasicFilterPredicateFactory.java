package com.pocketcombats.admin.core.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import org.springframework.core.convert.ConversionService;

public class BasicFilterPredicateFactory implements FilterPredicateFactory {

    private final ConversionService conversionService;
    private final Attribute<?, ?> attribute;

    public BasicFilterPredicateFactory(
            ConversionService conversionService,
            Attribute<?, ?> attribute
    ) {
        this.conversionService = conversionService;
        this.attribute = attribute;
    }

    @Override
    public Predicate createPredicate(CriteriaBuilder cb, Root<?> root, String value) {
        return cb.equal(root.get(attribute.getName()), conversionService.convert(value, attribute.getJavaType()));
    }
}
