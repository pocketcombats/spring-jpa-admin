package com.pocketcombats.admin.core;

import com.pocketcombats.admin.core.formatter.ValueFormatter;
import com.pocketcombats.admin.data.list.AdminEntityListEntry;
import com.pocketcombats.admin.util.EntityUtils;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.ObjectUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionService;

import java.util.List;

public class AdminModelListEntityMapper {

    private final EntityManager em;
    private final ConversionService conversionService;

    public AdminModelListEntityMapper(EntityManager em, ConversionService conversionService) {
        this.em = em;
        this.conversionService = conversionService;
    }

    public AdminEntityListEntry mapEntry(Object entity, List<AdminModelListField> fields) {
        String id = resolveId(entity);

        List<@Nullable Object> attributes = fields.stream()
                .<@Nullable Object>map(field -> fieldValue(field, entity))
                .toList();

        return new AdminEntityListEntry(id, attributes);
    }

    public @Nullable Object fieldValue(AdminModelListField field, Object entity) {
        Object value = field.valueAccessor().getValue(entity);
        ValueFormatter valueFormatter = field.valueFormatter();
        if (valueFormatter == null) {
            return value;
        } else {
            value = valueFormatter.format(value);
        }
        if (ObjectUtils.isEmpty(value)) {
            return field.emptyValue();
        } else {
            return value;
        }
    }

    private String resolveId(Object entity) {
        return EntityUtils.getEntityStringId(em, conversionService, entity);
    }

    /**
     * Human-readable representation of an entity: the value of the model's first list field,
     * falling back to the stringified entity id when the model declares no list fields.
     */
    public @Nullable Object entityRepresentation(AdminRegisteredModel model, Object entity) {
        List<AdminModelListField> listFields = model.listFields();
        if (listFields.isEmpty()) {
            return resolveId(entity);
        }
        return fieldValue(listFields.get(0), entity);
    }
}
