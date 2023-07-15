package com.pocketcombats.admin.core.action;

import com.pocketcombats.admin.history.AdminHistoryWriter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;

public class AdminModelDelegatingAction extends DelegatingAction {

    private final Object adminModel;

    public AdminModelDelegatingAction(AdminHistoryWriter historyWriter, Object adminModel, Method method) {
        super(historyWriter, method);

        this.adminModel = adminModel;
    }

    @Override
    protected void run(Method method, List<?> entities) {
        ReflectionUtils.invokeMethod(method, adminModel, entities);
    }
}
