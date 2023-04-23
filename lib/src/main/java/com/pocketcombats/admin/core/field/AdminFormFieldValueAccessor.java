package com.pocketcombats.admin.core.field;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import org.springframework.validation.BindingResult;

import java.util.Map;

public interface AdminFormFieldValueAccessor {

    String getName();

    String getDefaultTemplate();

    Class<?> getReaderJavaType();

    @Nullable
    Class<?> getWriterJavaType();

    boolean isWritable();

    /**
     * Returned value is not necessarily of type equal to {@link #getReaderJavaType()}.
     */
    Object readValue(Object instance);

    void setValue(Object instance, @Nullable String value, BindingResult bindingResult);

    Map<String, Object> getModelAttributes(EntityManager em);
}
