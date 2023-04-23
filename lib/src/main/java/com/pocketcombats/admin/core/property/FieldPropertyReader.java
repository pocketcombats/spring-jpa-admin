package com.pocketcombats.admin.core.property;

import org.springframework.beans.NotReadablePropertyException;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

public class FieldPropertyReader implements AdminModelPropertyReader {

    private final String propertyName;
    private final Class<?> javaType;
    private final Field field;

    public FieldPropertyReader(Field field) {
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
    public Object getValue(Object instance) {
        try {
            return field.get(instance);
        } catch (Exception ex) {
            throw new NotReadablePropertyException(instance.getClass(), propertyName, "", ex);
        }
    }
}
