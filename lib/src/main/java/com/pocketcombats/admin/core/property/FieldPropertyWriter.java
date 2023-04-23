package com.pocketcombats.admin.core.property;

import org.springframework.beans.NotWritablePropertyException;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

public class FieldPropertyWriter implements AdminModelPropertyWriter {

    private final String propertyName;
    private final Class<?> javaType;
    private final Field field;

    public FieldPropertyWriter(Field field) {
        this.propertyName = field.getName();
        this.javaType = field.getType();
        ReflectionUtils.makeAccessible(field);
        this.field = field;
    }

    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    @Override
    public void setValue(Object instance, Object value) {
        try {
            field.set(instance, value);
        } catch (Exception ex) {
            throw new NotWritablePropertyException(instance.getClass(), propertyName, "", ex);
        }
    }
}
