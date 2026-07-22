package com.pocketcombats.admin.test;

import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.core.permission.AdminPermissionService;

import java.util.HashSet;
import java.util.Set;

/** Permits everything until the name denies models view, edit, or create access. */
public final class StubPermissionService implements AdminPermissionService {

    private final Set<String> deniedView = new HashSet<>();
    private final Set<String> deniedEdit = new HashSet<>();
    private final Set<String> deniedCreate = new HashSet<>();

    public void deny(String... modelNames) {
        deniedView.addAll(Set.of(modelNames));
    }

    public void denyEdit(String... modelNames) {
        deniedEdit.addAll(Set.of(modelNames));
    }

    public void denyCreate(String... modelNames) {
        deniedCreate.addAll(Set.of(modelNames));
    }

    @Override
    public boolean canView(AdminRegisteredModel model) {
        return !deniedView.contains(model.modelName());
    }

    @Override
    public boolean canEdit(AdminRegisteredModel model) {
        return !deniedEdit.contains(model.modelName());
    }

    @Override
    public boolean canCreate(AdminRegisteredModel model) {
        return !deniedCreate.contains(model.modelName());
    }
}
