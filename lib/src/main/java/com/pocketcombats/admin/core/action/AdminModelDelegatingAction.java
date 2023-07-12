package com.pocketcombats.admin.core.action;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;

public class AdminModelDelegatingAction extends DelegatingAction {

    private final Object adminModel;

    public AdminModelDelegatingAction(Object adminModel, Method method) {
        super(method);

        this.adminModel = adminModel;
    }

    @Override
    protected void run(Method method, List<?> entities) {
        ReflectionUtils.invokeMethod(method, adminModel, entities);
    }
}
