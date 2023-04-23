package com.pocketcombats.admin.core;

import com.pocketcombats.admin.data.AdminModelInfo;

import java.util.List;

public interface AdminModelRegistry {

    AdminRegisteredModel resolve(String modelName) throws UnknownModelException;

    List<AdminModelInfo> listModels();
}
