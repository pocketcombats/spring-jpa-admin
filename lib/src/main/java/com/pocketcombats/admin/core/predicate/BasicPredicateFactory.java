package com.pocketcombats.admin.core.predicate;

import com.pocketcombats.admin.util.ConversionUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import org.jspecify.annotations.Nullable;
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

        Object value = ConversionUtils.tryConvert(conversionService, rawValue, attribute.getJavaType());
        if (value == null) {
            return cb.disjunction();
        }
        return cb.equal(root.get(attribute.getName()), value);
    }
}
