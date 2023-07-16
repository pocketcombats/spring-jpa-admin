package com.pocketcombats.admin.core;

import com.pocketcombats.admin.data.AdminModelInfo;
import com.pocketcombats.admin.data.AdminModelsGroup;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AdminModelRegistryImpl implements AdminModelRegistry {

    private final List<AdminModelsGroup> modelGroups;
    private final Map<String, AdminRegisteredModel> modelsByName;

    public AdminModelRegistryImpl(
            // Category -> [Model]
            Map<String, List<AdminRegisteredModel>> models
    ) {
        this.modelGroups = models.entrySet().stream()
                .map(entry -> new AdminModelsGroup(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(model -> new AdminModelInfo(model.label(), model.modelName()))
                                .toList()
                ))
                .toList();
        this.modelsByName = models.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toMap(
                        AdminRegisteredModel::modelName,
                        Function.identity()
                ));
    }

    @Override
    public List<AdminModelsGroup> getModelGroups() {
        return modelGroups;
    }

    @Override
    public AdminRegisteredModel resolve(String modelName) throws UnknownModelException {
        if (!modelsByName.containsKey(modelName)) {
            throw new UnknownModelException();
        }
        return modelsByName.get(modelName);
    }
}
