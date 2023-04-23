package com.pocketcombats.admin.core.property;

import org.springframework.beans.NotWritablePropertyException;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

public class MethodPropertyWriter implements AdminModelPropertyWriter {

    private final String propertyName;
    private final Method writeMethod;
    private final Class<?> javaType;

    public MethodPropertyWriter(String propertyName, Method writeMethod) {
        this.propertyName = propertyName;
        this.writeMethod = writeMethod;
        ReflectionUtils.makeAccessible(writeMethod);
        this.javaType = writeMethod.getParameterTypes()[0];
    }

    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    @Override
    public void setValue(Object instance, Object value) {
        try {
            writeMethod.invoke(instance, value);
        } catch (Exception ex) {
            throw new NotWritablePropertyException(instance.getClass(), propertyName, "", ex);
        }
    }
}
