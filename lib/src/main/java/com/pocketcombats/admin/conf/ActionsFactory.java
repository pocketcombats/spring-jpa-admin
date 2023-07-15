package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.AdminAction;
import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.core.action.AdminModelAction;
import com.pocketcombats.admin.core.action.AdminModelDelegatingAction;
import com.pocketcombats.admin.core.action.StaticMethodDelegatingAction;
import com.pocketcombats.admin.history.AdminHistoryWriter;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ActionsFactory {

    private final AdminHistoryWriter historyWriter;
    private final List<AdminModelAction> defaultActions;

    public ActionsFactory(AdminHistoryWriter historyWriter, List<AdminModelAction> defaultActions) {
        this.historyWriter = historyWriter;
        this.defaultActions = new ArrayList<>(defaultActions);
        // Default actions may have duplicate ids.
        // Actions with lower @Order will override those with higher order.
        Collections.reverse(this.defaultActions);
    }

    public Map<String, AdminModelAction> createActions(
            AdminModel modelAnnotation,
            Class<?> targetClass,
            @Nullable Class<?> adminModelClass,
            @Nullable Object adminModelBean
    ) {
        Map<String, AdminModelAction> actions = new LinkedHashMap<>();
        for (var defaultAction : defaultActions) {
            actions.put(defaultAction.getId(), defaultAction);
        }
        // @AdminActions defined on @Entity level may override default ones
        for (Method actionMethod : findActionMethods(targetClass)) {
            if (!Modifier.isStatic(actionMethod.getModifiers())) {
                throw new IllegalStateException(
                        "Entity methods annotated with @AdminAction MUST be static. " +
                                "Violating method: " + targetClass.getName() + "#" + actionMethod.getName()
                );
            }
            actions.put(actionMethod.getName(), new StaticMethodDelegatingAction(historyWriter, actionMethod));
        }
        if (adminModelClass != null) {
            // @AdminActions defined on admin model level have the highest precedence
            for (Method actionMethod : findActionMethods(adminModelClass)) {
                actions.put(
                        actionMethod.getName(),
                        new AdminModelDelegatingAction(
                                historyWriter,
                                adminModelBean,
                                actionMethod
                        )
                );
            }
        }
        for (String disableAction : modelAnnotation.disableActions()) {
            actions.remove(disableAction);
        }
        return actions;
    }

    private static List<Method> findActionMethods(Class<?> targetClass) {
        List<Method> methods = MethodUtils.getMethodsListWithAnnotation(targetClass, AdminAction.class, false, true);
        for (Method method : methods) {
            if (method.getParameterCount() != 1 || !method.getParameterTypes()[0].isAssignableFrom(List.class)) {
                throw new IllegalStateException(
                        "Methods annotated with @AdminAction MUST accept single argument of type List. " +
                                "Violating method: " + targetClass.getName() + "#" + method.getName()
                );
            }
            AdminAction annotation = method.getAnnotation(AdminAction.class);
            if (annotation.localize() && "".equals(annotation.label())) {
                throw new IllegalStateException("AdminAction is marked for localization but has no explicit label." +
                        "Violating method: " + targetClass.getName() + "#" + method.getName());
            }
        }
        return methods;
    }
}
