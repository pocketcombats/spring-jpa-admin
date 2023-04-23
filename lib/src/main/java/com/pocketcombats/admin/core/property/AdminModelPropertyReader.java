package com.pocketcombats.admin.core.property;

public interface AdminModelPropertyReader {

    Class<?> getJavaType();

    Object getValue(Object instance);
}
