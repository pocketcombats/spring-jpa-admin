package com.pocketcombats.admin.core;

import com.pocketcombats.admin.data.list.AdminEntityListEntry;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminModelListEntityMapper {

    private final EntityManager em;
    private final ConversionService conversionService;

    public AdminModelListEntityMapper(EntityManager em, ConversionService conversionService) {
        this.em = em;
        this.conversionService = conversionService;
    }

    public AdminEntityListEntry mapEntry(Object entity, List<AdminModelListField> fields) {
        String id = resolveId(entity);

        List<Object> attributes = fields.stream()
                .map(field -> fieldValue(field, entity))
                .toList();

        return new AdminEntityListEntry(id, attributes);
    }

    public Object fieldValue(AdminModelListField field, Object entity) {
        Object value = field.valueAccessor().getValue(entity);
        if (field.valueFormatter() == null) {
            return value;
        } else {
            value = field.valueFormatter().format(value);
        }
        if (ObjectUtils.isEmpty(value)) {
            return field.emptyValue();
        } else {
            return value;
        }
    }

    private String resolveId(Object entity) {
        Object identifier = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        return conversionService.convert(identifier, String.class);
    }
}
