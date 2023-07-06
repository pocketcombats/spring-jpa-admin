package com.pocketcombats.admin.core.filter;

import com.pocketcombats.admin.core.formatter.ValueFormatter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.core.convert.ConversionService;

import java.util.List;

public class ToOneFilterOptionsCollector extends AbstractAttributeFilterOptionsCollector {

    private final EntityManager em;
    private final ConversionService conversionService;
    private final ValueFormatter valueFormatter;

    public ToOneFilterOptionsCollector(
            EntityManager em,
            ConversionService conversionService,
            EntityType<?> entityType,
            Attribute<?, ?> attribute,
            ValueFormatter valueFormatter
    ) {
        super(em, entityType, attribute);
        this.em = em;
        this.conversionService = conversionService;
        this.valueFormatter = valueFormatter;
    }

    @Override
    protected List<ModelFilterOption> mapResults(List<?> resultList) {
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
