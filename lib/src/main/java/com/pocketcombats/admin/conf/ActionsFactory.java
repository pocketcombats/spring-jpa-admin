package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.AdminAction;
import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.core.action.AdminModelAction;
import com.pocketcombats.admin.core.action.AdminModelDelegatingAction;
import com.pocketcombats.admin.core.action.StaticMethodDelegatingAction;
import com.pocketcombats.admin.history.AdminHistoryWriter;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /* package */ Map<String, AdminModelAction> createActions(
            AdminModel modelAnnotation,
            Class<?> targetClass,
            @Nullable AdminModelBean adminModelBean
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
                                "In Kotlin, declare the action inside a companion object " +
                                "or on the admin model class. " +
                                "Violating method: " + targetClass.getName() + "#" + actionMethod.getName()
                );
            }
            actions.put(actionMethod.getName(), new StaticMethodDelegatingAction(historyWriter, actionMethod));
        }
        // Kotlin companion object functions compile to instance methods on the companion class,
        // so they are invisible to the static scan above
        Object companion = findCompanionInstance(targetClass);
        if (companion != null) {
            for (Method actionMethod : findActionMethods(companion.getClass())) {
                if (Modifier.isStatic(actionMethod.getModifiers())) {
                    actions.put(actionMethod.getName(), new StaticMethodDelegatingAction(historyWriter, actionMethod));
                } else {
                    actions.put(
                            actionMethod.getName(),
                            new AdminModelDelegatingAction(historyWriter, companion, actionMethod)
                    );
                }
            }
        }
        if (adminModelBean != null) {
            // @AdminActions defined on admin model level have the highest precedence
            for (Method actionMethod : findActionMethods(adminModelBean.modelClass())) {
                actions.put(
                        actionMethod.getName(),
                        new AdminModelDelegatingAction(
                                historyWriter,
                                adminModelBean.instance(),
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

    /**
     * A Kotlin companion object is compiled to a nested class plus a static final field on the
     * enclosing class, both bearing the companion's name ({@code Companion} unless named explicitly).
     */
    private static @Nullable Object findCompanionInstance(Class<?> targetClass) {
        if (!isKotlinClass(targetClass)) {
            // The companion shape below is also a common Java idiom (a same-named nested holder
            // constant): scanning such a holder would register its methods as actions and
            // makeAccessible could fail under strong encapsulation, so only Kotlin classes —
            // recognized by the kotlin.Metadata annotation the compiler stamps on every one of
            // them — are searched for companions.
            return null;
        }
        for (Field field : targetClass.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)
                    && field.getType().getEnclosingClass() == targetClass
                    && field.getName().equals(field.getType().getSimpleName())) {
                ReflectionUtils.makeAccessible(field);
                return ReflectionUtils.getField(field, null);
            }
        }
        return null;
    }

    // By name, not by class literal: the library must not depend on the Kotlin stdlib.
    private static boolean isKotlinClass(Class<?> targetClass) {
        for (Annotation annotation : targetClass.getAnnotations()) {
            if (annotation.annotationType().getName().equals("kotlin.Metadata")) {
                return true;
            }
        }
        return false;
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
        }
        return methods;
    }
}
