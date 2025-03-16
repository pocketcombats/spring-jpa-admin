package com.pocketcombats.admin.core;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AdminModelRegistryImpl implements AdminModelRegistry {

    private final Map<String, List<AdminRegisteredModel>> categorizedModels;
    private final Map<String, AdminRegisteredModel> modelsByName;

    public AdminModelRegistryImpl(
            // Category -> [Model]
            Map<String, List<AdminRegisteredModel>> models
    ) {
        this.categorizedModels = models;
        this.modelsByName = models.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toMap(
                        AdminRegisteredModel::modelName,
                        Function.identity()
                ));
    }

    @Override
    public Map<String, List<AdminRegisteredModel>> getCategorizedModels() {
        return categorizedModels;
    }

    @Override
    public AdminRegisteredModel resolve(String modelName) throws UnknownModelException {
        if (!modelsByName.containsKey(modelName)) {
            throw new UnknownModelException();
        }
        return modelsByName.get(modelName);
    }
}
