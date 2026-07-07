package com.pocketcombats.admin.core.property;

import org.jspecify.annotations.Nullable;

public interface AdminModelPropertyWriter {

    Class<?> getJavaType();

    void setValue(Object instance, @Nullable Object value);
}
