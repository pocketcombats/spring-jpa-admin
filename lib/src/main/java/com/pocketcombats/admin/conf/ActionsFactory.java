package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.core.action.AdminModelAction;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ActionsFactory {

    private final List<AdminModelAction> defaultActions;

    public ActionsFactory(List<AdminModelAction> defaultActions) {
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
        Map<String, AdminModelAction> actions = new HashMap<>();
        for (var defaultAction : defaultActions) {
            actions.put(defaultAction.getId(), defaultAction);
        }
        return actions;
    }
}
