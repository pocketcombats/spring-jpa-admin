package com.pocketcombats.admin.core.property;

import org.springframework.beans.NotWritablePropertyException;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

public class AdminModelDelegatingPropertyWriter implements AdminModelPropertyWriter {

    private final String propertyName;
    private final Object adminModel;
    private final Method method;
    private final Class<?> javaType;

    public AdminModelDelegatingPropertyWriter(String propertyName, Object adminModel, Method method) {
        this.propertyName = propertyName;
        this.adminModel = adminModel;
        this.method = method;
        ReflectionUtils.makeAccessible(method);
        this.javaType = method.getReturnType();
    }

    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    @Override
    public void setValue(Object instance, Object value) {
        try {
            method.invoke(adminModel, instance, value);
        } catch (Exception ex) {
            throw new NotWritablePropertyException(instance.getClass(), propertyName, "", ex);
        }
    }
}

