package com.pocketcombats.admin.core;

import com.pocketcombats.admin.data.AdminModelsGroup;

import java.util.List;

public interface AdminModelRegistry {

    AdminRegisteredModel resolve(String modelName) throws UnknownModelException;

    List<AdminModelsGroup> getModelGroups();
}
