package com.pocketcombats.admin.util;

import jakarta.persistence.EntityManager;
import org.springframework.core.convert.ConversionService;

import java.util.Objects;

public class EntityUtils {

    private EntityUtils() {
    }

    /**
     * Resolves the identifier of a managed entity and converts it to its string form.
     */
    public static String getEntityStringId(EntityManager em, ConversionService conversionService, Object entity) {
        Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        return Objects.requireNonNull(conversionService.convert(id, String.class));
    }
}
