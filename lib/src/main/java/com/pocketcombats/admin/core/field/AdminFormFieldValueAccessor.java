package com.pocketcombats.admin.core.field;

import org.jspecify.annotations.Nullable;

import java.util.Map;

public interface AdminFormFieldValueAccessor {

    String getName();

    String getDefaultTemplate();

    Class<?> getReaderJavaType();

    /**
     * The type this accessor writes, or {@link Void#TYPE} when the field is not writable.
     * Use {@link #isWritable()}, not a comparison against this value.
     */
    Class<?> getWriterJavaType();

    boolean isWritable();

    /**
     * Returned value is not necessarily of type equal to {@link #getReaderJavaType()}.
     */
    @Nullable Object readValue(Object instance);

    /**
     * Template attributes; may depend on the edited instance (a fresh entity when creating).
     */
    Map<String, ? extends @Nullable Object> getModelAttributes(Object instance);
}
