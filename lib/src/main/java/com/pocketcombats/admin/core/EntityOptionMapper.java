package com.pocketcombats.admin.core;

import com.pocketcombats.admin.core.formatter.ValueFormatter;
import com.pocketcombats.admin.util.EntityUtils;
import jakarta.persistence.EntityManager;
import org.springframework.core.convert.ConversionService;

/**
 * Maps a relation target entity to the string id / display label pair used by option-producing
 * widgets (selects, multiselects, filters, autocomplete).
 */
public class EntityOptionMapper {

    private final EntityManager em;
    private final ConversionService conversionService;
    private final ValueFormatter valueFormatter;

    public EntityOptionMapper(
            EntityManager em,
            ConversionService conversionService,
            ValueFormatter valueFormatter
    ) {
        this.em = em;
        this.conversionService = conversionService;
        this.valueFormatter = valueFormatter;
    }

    /**
     * The entity's identifier in string form.
     */
    public String stringId(Object entity) {
        return EntityUtils.getEntityStringId(em, conversionService, entity);
    }

    /**
     * The entity's display label, per the field's configured representation.
     */
    public String label(Object entity) {
        return valueFormatter.format(entity);
    }
}
