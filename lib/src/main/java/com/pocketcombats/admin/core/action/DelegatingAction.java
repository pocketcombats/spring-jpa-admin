package com.pocketcombats.admin.core.action;

import com.pocketcombats.admin.AdminAction;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.util.AdminStringUtils;
import jakarta.persistence.EntityManager;

import java.lang.reflect.Method;
import java.util.List;

public abstract class DelegatingAction implements AdminModelAction {

    private final Method method;
    private final AdminAction actionConfig;
    private final String action;

    private final String label;
    private final String description;

    public DelegatingAction(Method method) {
        this.method = method;
        this.actionConfig = method.getAnnotation(AdminAction.class);
        this.action = method.getName();

        if (!"".equals(actionConfig.label())) {
            this.label = actionConfig.label();
        } else {
            this.label = AdminStringUtils.toHumanReadableName(method.getName());
        }
        this.description = actionConfig.description();
    }

    @Override
    public String getId() {
        return action;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isLocalized() {
        return actionConfig.localize();
    }

    @Override
    public final void run(EntityManager em, AdminRegisteredModel model, List<?> entities) {
        // TODO: audit
        run(method, entities);
    }

    protected abstract void run(Method method, List<?> entities);
}
