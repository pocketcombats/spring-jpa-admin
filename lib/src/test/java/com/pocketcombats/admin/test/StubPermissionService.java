package com.pocketcombats.admin.test;

import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.core.permission.AdminPermissionService;

import java.util.HashSet;
import java.util.Set;

/** Permits everything until models are {@link #deny denied} view access by name. */
public final class StubPermissionService implements AdminPermissionService {

    private final Set<String> denied = new HashSet<>();

    public void deny(String... modelNames) {
        denied.addAll(Set.of(modelNames));
    }

    @Override
    public boolean canView(AdminRegisteredModel model) {
        return !denied.contains(model.modelName());
    }

    @Override
    public boolean canEdit(AdminRegisteredModel model) {
        return true;
    }

    @Override
    public boolean canCreate(AdminRegisteredModel model) {
        return true;
    }
}
