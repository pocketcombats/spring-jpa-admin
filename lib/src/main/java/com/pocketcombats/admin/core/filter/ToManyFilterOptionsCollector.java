package com.pocketcombats.admin.core.filter;

import com.pocketcombats.admin.core.formatter.ValueFormatter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.PluralAttribute;
import org.springframework.core.convert.ConversionService;

import java.util.List;

public class ToManyFilterOptionsCollector implements FilterOptionsCollector {

    private final EntityManager em;
    private final ConversionService conversionService;
    private final EntityType<?> entityType;
    private final PluralAttribute<?, ?, ?> attribute;
    private final ValueFormatter valueFormatter;

    private final Class<?> attributeElementJavaType;

    public ToManyFilterOptionsCollector(
            EntityManager em,
            ConversionService conversionService,
            EntityType<?> entityType,
            Attribute<?, ?> attribute,
            ValueFormatter valueFormatter
    ) {
        this.em = em;
        this.conversionService = conversionService;
        this.entityType = entityType;
        this.attribute = (PluralAttribute<?, ?, ?>) attribute;
        this.valueFormatter = valueFormatter;

        this.attributeElementJavaType = this.attribute.getElementType().getJavaType();
    }

    @Override
    public List<ModelFilterOption> collectOptions() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<?> query = cb.createQuery(attributeElementJavaType);
        Root<?> root = query.from(entityType);
        query.select(root.get(attribute.getName())).distinct(true);
        List<?> resultList = em.createQuery(query).getResultList();
        return resultList.stream()
                .map(relation -> new ModelFilterOption(getEntityStringValue(relation), getEntityStringId(relation)))
                .toList();
    }

    protected String getEntityStringId(Object entity) {
        Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        return conversionService.convert(id, String.class);
    }

    protected String getEntityStringValue(Object entity) {
        return valueFormatter.format(entity);
    }
}
