package com.pocketcombats.admin.core.predicate;


import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.core.convert.ConversionService;

public class ToOnePredicateFactory implements ValuePredicateFactory {

    private final EntityManager em;
    private final ConversionService conversionService;
    private final SingularAttribute<?, ?> attribute;

    private final Class<?> attributeIdType;

    public ToOnePredicateFactory(
            EntityManager em,
            ConversionService conversionService,
            Attribute<?, ?> attribute
    ) {
        this.em = em;
        this.conversionService = conversionService;
        this.attribute = (SingularAttribute<?, ?>) attribute;

        this.attributeIdType = ((IdentifiableType<?>) this.attribute.getType()).getIdType().getJavaType();
    }

    @Override
    public Predicate createPredicate(CriteriaBuilder cb, Root<?> root, @Nullable Object value) {
        if (value == null) {
            return cb.isNull(root.get(attribute.getName()));
        }

        Object reference;
        if (attribute.getJavaType().isAssignableFrom(value.getClass())) {
            reference = value;
        } else {
            Object id = attributeIdType.isAssignableFrom(value.getClass())
                    ? value
                    : conversionService.convert(value, attributeIdType);
            reference = em.getReference(attribute.getJavaType(), id);
        }
        return cb.equal(root.get(attribute.getName()), reference);
    }
}
