package com.pocketcombats.admin.core.property;

import org.springframework.beans.NotReadablePropertyException;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

public class AdminModelDelegatingPropertyReader implements AdminModelPropertyReader {

    private final String propertyName;
    private final Object adminModel;
    private final Method method;
    private final Class<?> javaType;

    public AdminModelDelegatingPropertyReader(String propertyName, Object adminModel, Method method) {
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
    public Object getValue(Object instance) {
        try {
            return method.invoke(adminModel, instance);
        } catch (Exception ex) {
            throw new NotReadablePropertyException(instance.getClass(), propertyName, "", ex);
        }
    }
}
