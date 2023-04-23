package com.pocketcombats.admin.core.property;

import org.springframework.beans.NotReadablePropertyException;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

public class MethodPropertyReader implements AdminModelPropertyReader {

    private final String propertyName;
    private final Method readMethod;
    private final Class<?> javaType;

    public MethodPropertyReader(String propertyName, Method readMethod) {
        this.propertyName = propertyName;
        this.readMethod = readMethod;
        ReflectionUtils.makeAccessible(readMethod);
        this.javaType = readMethod.getReturnType();
    }

    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    @Override
    public Object getValue(Object instance) {
        try {
            return readMethod.invoke(instance);
        } catch (Exception ex) {
            throw new NotReadablePropertyException(instance.getClass(), propertyName, "", ex);
        }
    }
}

