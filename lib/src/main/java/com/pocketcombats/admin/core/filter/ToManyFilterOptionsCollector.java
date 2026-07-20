package com.pocketcombats.admin.core.filter;

import com.pocketcombats.admin.core.EntityOptionMapper;
import com.pocketcombats.admin.core.PredicateFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.PluralAttribute;

import java.util.Comparator;
import java.util.List;

public class ToManyFilterOptionsCollector implements FilterOptionsCollector {

    private final EntityManager em;
    private final EntityType<?> entityType;
    private final PluralAttribute<?, ?, ?> attribute;
    private final EntityOptionMapper optionMapper;

    private final Class<?> attributeElementJavaType;

    public ToManyFilterOptionsCollector(
            EntityManager em,
            EntityType<?> entityType,
            Attribute<?, ?> attribute,
            EntityOptionMapper optionMapper
    ) {
        this.em = em;
        this.entityType = entityType;
        this.attribute = (PluralAttribute<?, ?, ?>) attribute;
        this.optionMapper = optionMapper;

        this.attributeElementJavaType = this.attribute.getElementType().getJavaType();
    }

    @Override
    public List<ModelFilterOption> collectOptions(PredicateFactory predicateFactory) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<?> query = cb.createQuery(attributeElementJavaType);
        Root<?> root = query.from(entityType);
        query.where(predicateFactory.create(root));
        query.select(root.get(attribute.getName())).distinct(true);
        List<?> resultList = em.createQuery(query).getResultList();
        return resultList.stream()
                .map(relation -> new ModelFilterOption(optionMapper.label(relation), optionMapper.stringId(relation)))
                .sorted(Comparator.comparing(ModelFilterOption::label))
                .toList();
    }
}
