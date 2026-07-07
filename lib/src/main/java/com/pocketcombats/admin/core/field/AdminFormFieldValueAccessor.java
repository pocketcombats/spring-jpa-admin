package com.pocketcombats.admin.core.field;

import org.jspecify.annotations.Nullable;

import java.util.Map;

public interface AdminFormFieldValueAccessor {

    String getName();

    String getDefaultTemplate();

    Class<?> getReaderJavaType();

    @Nullable Class<?> getWriterJavaType();

    boolean isWritable();

    /**
     * Returned value is not necessarily of type equal to {@link #getReaderJavaType()}.
     */
    @Nullable Object readValue(Object instance);

    Map<String, Object> getModelAttributes();
}
