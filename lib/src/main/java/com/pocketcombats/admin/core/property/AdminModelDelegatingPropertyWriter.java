package com.pocketcombats.admin.core.property;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

public class AdminModelDelegatingPropertyWriter implements AdminModelPropertyWriter {

    private final String propertyName;
    private final Object adminModel;
    private final Method method;
    private final Class<?> javaType;

    public AdminModelDelegatingPropertyWriter(String propertyName, Object adminModel, Method method) {
        if (method.getParameterCount() != 2) {
            throw new IllegalArgumentException(
                    "Admin model property writer method must accept (entity, value): " + method
            );
        }
        this.propertyName = propertyName;
        this.adminModel = adminModel;
        this.method = method;
        ReflectionUtils.makeAccessible(method);
        this.javaType = method.getParameterTypes()[1];
    }

    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    @Override
    public void setValue(Object instance, @Nullable Object value) {
        try {
            method.invoke(adminModel, instance, value);
        } catch (Exception ex) {
            throw new NotWritablePropertyException(instance.getClass(), propertyName, "", ex);
        }
    }
}

