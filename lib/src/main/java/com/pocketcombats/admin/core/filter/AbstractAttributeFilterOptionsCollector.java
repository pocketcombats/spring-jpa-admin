package com.pocketcombats.admin.core.filter;

import com.pocketcombats.admin.core.PredicateFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;

import java.util.Comparator;
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
    public List<ModelFilterOption> collectOptions(PredicateFactory predicateFactory) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Class<?> attributeJavaType = attribute.getJavaType();
        CriteriaQuery<?> query = cb.createQuery(attributeJavaType);
        Root<?> root = query.from(entityType);
        query.select(root.get(attribute.getName())).distinct(true);
        Predicate where = cb.and(
                predicateFactory.create(root),
                cb.isNotNull(root.get(attribute.getName()))
        );
        query.where(where);
        boolean isAssociation = attribute.isAssociation();
        if (!isAssociation) {
            // Deterministic option order for basic attributes
            query.orderBy(cb.asc(root.get(attribute.getName())));
        }
        List<?> resultList = em.createQuery(query).getResultList();
        List<ModelFilterOption> options = mapResults(resultList);
        if (isAssociation) {
            // Association options have no meaningful SQL order and are sorted by their formatted label
            options = options.stream()
                    .sorted(Comparator.comparing(ModelFilterOption::label))
                    .toList();
        }
        return options;
    }

    protected abstract List<ModelFilterOption> mapResults(List<?> resultList);
}
