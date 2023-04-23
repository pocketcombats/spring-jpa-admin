package com.pocketcombats.admin.core;

import com.pocketcombats.admin.data.AdminModelInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AdminModelRegistryImpl implements AdminModelRegistry {

    private final Map<String, AdminRegisteredModel> models;

    public AdminModelRegistryImpl(
            Collection<AdminRegisteredModel> models
    ) {
        this.models = models.stream()
                .collect(Collectors.toMap(
                        AdminRegisteredModel::modelName,
                        Function.identity()
                ));
    }

    @Override
    public List<AdminModelInfo> listModels() {
        return models.values().stream()
                .map(model -> new AdminModelInfo(model.label(), model.modelName()))
                .toList();
    }

    @Override
    public AdminRegisteredModel resolve(String modelName) throws UnknownModelException {
        if (!models.containsKey(modelName)) {
            throw new UnknownModelException();
        }
        return models.get(modelName);
    }
}
