package com.pocketcombats.admin.core;

import java.util.List;
import java.util.Map;

public interface AdminModelRegistry {

    AdminRegisteredModel resolve(String modelName) throws UnknownModelException;

    Map<String, List<AdminRegisteredModel>> getCategorizedModels();
}
