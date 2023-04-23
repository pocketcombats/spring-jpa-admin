package com.pocketcombats.admin.core.property;

public interface AdminModelPropertyWriter {

    Class<?> getJavaType();

    void setValue(Object instance, Object value);
}
