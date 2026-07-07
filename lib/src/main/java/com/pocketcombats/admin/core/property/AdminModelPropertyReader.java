package com.pocketcombats.admin.core.property;

import org.jspecify.annotations.Nullable;

public interface AdminModelPropertyReader {

    Class<?> getJavaType();

    @Nullable Object getValue(Object instance);
}
