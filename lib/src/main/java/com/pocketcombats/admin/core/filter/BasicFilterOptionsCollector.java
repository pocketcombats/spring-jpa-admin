package com.pocketcombats.admin.core.filter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.core.convert.ConversionService;

import java.util.List;

public class BasicFilterOptionsCollector extends AbstractAttributeFilterOptionsCollector {

    private final ConversionService conversionService;

    public BasicFilterOptionsCollector(
            EntityManager em,
            ConversionService conversionService,
            EntityType<?> entityType,
            Attribute<?, ?> attribute
    ) {
        super(em, entityType, attribute);

        this.conversionService = conversionService;
    }

    @Override
    protected List<ModelFilterOption> mapResults(List<?> resultList) {
        return resultList.stream()
                .map(value -> conversionService.convert(value, String.class))
                .map(value -> new ModelFilterOption(value, value))
                .toList();
    }
}
