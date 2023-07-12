package com.pocketcombats.admin.core.action;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;

public class StaticMethodDelegatingAction extends DelegatingAction {

    public StaticMethodDelegatingAction(Method method) {
        super(method);
    }

    @Override
    protected void run(Method method, List<?> entities) {
        ReflectionUtils.invokeMethod(method, null, entities);
    }
}
