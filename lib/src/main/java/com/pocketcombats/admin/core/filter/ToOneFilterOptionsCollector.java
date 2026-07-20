package com.pocketcombats.admin.core.filter;

import com.pocketcombats.admin.core.EntityOptionMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;

import java.util.List;

public class ToOneFilterOptionsCollector extends AbstractAttributeFilterOptionsCollector {

    private final EntityOptionMapper optionMapper;

    public ToOneFilterOptionsCollector(
            EntityManager em,
            EntityType<?> entityType,
            Attribute<?, ?> attribute,
            EntityOptionMapper optionMapper
    ) {
        super(em, entityType, attribute);
        this.optionMapper = optionMapper;
    }

    @Override
    protected List<ModelFilterOption> mapResults(List<?> resultList) {
        return resultList.stream()
                .map(relation -> new ModelFilterOption(optionMapper.label(relation), optionMapper.stringId(relation)))
                .toList();
    }
}
