package com.pocketcombats.admin.core.predicate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.PluralAttribute;
import org.springframework.core.convert.ConversionService;

public class ToManyPredicateFactory implements ValuePredicateFactory {

    private final EntityManager em;
    private final ConversionService conversionService;
    private final PluralAttribute<?, ?, ?> attribute;

    private final Class<?> attributeElementType;
    private final Class<?> attributeIdType;

    public ToManyPredicateFactory(
            EntityManager em,
            ConversionService conversionService,
            Attribute<?, ?> attribute
    ) {
        this.em = em;
        this.conversionService = conversionService;
        this.attribute = (PluralAttribute<?, ?, ?>) attribute;

        IdentifiableType<?> elementType = (IdentifiableType<?>) this.attribute.getElementType();
        this.attributeElementType = elementType.getJavaType();
        this.attributeIdType = elementType.getIdType().getJavaType();
    }

    @Override
    public Predicate createPredicate(CriteriaBuilder cb, Root<?> root, Object value) {
        Object id = attributeIdType.isAssignableFrom(value.getClass())
                ? value
                : conversionService.convert(value, attributeIdType);
        Object reference = em.getReference(attributeElementType, id);
        return cb.isMember(reference, root.get(attribute.getName()));
    }
}
