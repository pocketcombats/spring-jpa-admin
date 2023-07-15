package com.pocketcombats.admin.core.field;

import jakarta.annotation.Nullable;

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

    Map<String, Object> getModelAttributes();
}
