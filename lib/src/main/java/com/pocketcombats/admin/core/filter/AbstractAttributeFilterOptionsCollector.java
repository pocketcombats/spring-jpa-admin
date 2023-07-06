package com.pocketcombats.admin.core.filter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;

import java.util.List;

public abstract class AbstractAttributeFilterOptionsCollector implements FilterOptionsCollector {

    private final EntityManager em;
    private final EntityType<?> entityType;
    private final Attribute<?, ?> attribute;

    public AbstractAttributeFilterOptionsCollector(
            EntityManager em,
            EntityType<?> entityType,
            Attribute<?, ?> attribute
    ) {
        this.em = em;
        this.entityType = entityType;
        this.attribute = attribute;
    }

    @Override
    public List<ModelFilterOption> collectOptions() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Class<?> attributeJavaType = attribute.getJavaType();
        CriteriaQuery<?> query = cb.createQuery(attributeJavaType);
        Root<?> root = query.from(entityType);
        query.select(root.get(attribute.getName())).distinct(true);
        query.where(cb.isNotNull(root.get(attribute.getName())));
        List<?> resultList = em.createQuery(query).getResultList();
        return mapResults(resultList);
    }

    protected abstract List<ModelFilterOption> mapResults(List<?> resultList);
}
