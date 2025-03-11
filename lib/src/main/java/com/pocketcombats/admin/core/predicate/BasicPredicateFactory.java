package com.pocketcombats.admin.core.predicate;

import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import org.springframework.core.convert.ConversionService;

public class BasicPredicateFactory implements ValuePredicateFactory {

    private final ConversionService conversionService;
    private final Attribute<?, ?> attribute;

    public BasicPredicateFactory(
            ConversionService conversionService,
            Attribute<?, ?> attribute
    ) {
        this.conversionService = conversionService;
        this.attribute = attribute;
    }

    @Override
    public Predicate createPredicate(CriteriaBuilder cb, Root<?> root, @Nullable Object rawValue) {
        if (rawValue == null) {
            return cb.isNull(root.get(attribute.getName()));
        }

        Object value = attribute.getJavaType().isAssignableFrom(rawValue.getClass())
                ? rawValue
                : conversionService.convert(rawValue, attribute.getJavaType());
        return cb.equal(root.get(attribute.getName()), value);
    }
}
