package com.pocketcombats.admin.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AdminModelRegistryImpl implements AdminModelRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(AdminModelRegistryImpl.class);

    private final Map<String, List<AdminRegisteredModel>> categorizedModels;
    private final Map<String, AdminRegisteredModel> modelsByName;
    private final Map<Class<?>, List<AdminRegisteredModel>> modelsByEntityClass;

    public AdminModelRegistryImpl(
            // Category -> [Model]
            Map<String, List<AdminRegisteredModel>> models
    ) {
        this.categorizedModels = models;
        List<AdminRegisteredModel> allModels = models.values().stream().flatMap(List::stream).toList();
        this.modelsByName = allModels.stream()
                .collect(Collectors.toMap(
                        AdminRegisteredModel::modelName,
                        Function.identity(),
                        (first, second) -> {
                            throw new IllegalStateException("Duplicate admin model name: " + first.modelName());
                        }
                ));
        this.modelsByEntityClass = allModels.stream()
                .collect(Collectors.groupingBy(model -> model.entityDetails().entityClass()));
        for (Map.Entry<Class<?>, List<AdminRegisteredModel>> entry : modelsByEntityClass.entrySet()) {
            List<AdminRegisteredModel> registrations = entry.getValue();
            if (registrations.size() > 1) {
                registrations.sort(primaryOrder(entry.getKey()));
                LOG.warn(
                        "Entity {} is registered as admin models {}; lookups by entity class resolve to \"{}\"",
                        entry.getKey().getName(),
                        registrations.stream()
                                .map(model -> "\"" + model.modelName() + "\"")
                                .collect(Collectors.joining(", ")),
                        registrations.get(0).modelName()
                );
            }
        }
        modelsByEntityClass.replaceAll((entityClass, registrations) -> List.copyOf(registrations));
    }

    // Deterministic primary order regardless of scan/registration order: the default-named
    // registration (entity's simple class name) first, then custom names lexicographically.
    private static Comparator<AdminRegisteredModel> primaryOrder(Class<?> entityClass) {
        String defaultName = entityClass.getSimpleName();
        return Comparator.comparing((AdminRegisteredModel model) -> !model.modelName().equals(defaultName))
                .thenComparing(AdminRegisteredModel::modelName);
    }

    @Override
    public Map<String, List<AdminRegisteredModel>> getCategorizedModels() {
        return categorizedModels;
    }

    @Override
    public AdminRegisteredModel resolve(String modelName) throws UnknownModelException {
        AdminRegisteredModel model = modelsByName.get(modelName);
        if (model == null) {
            throw new UnknownModelException();
        }
        return model;
    }

    @Override
    public Optional<AdminRegisteredModel> findByEntityClass(Class<?> entityClass) {
        List<AdminRegisteredModel> registrations = modelsByEntityClass.get(entityClass);
        return registrations == null ? Optional.empty() : Optional.of(registrations.get(0));
    }

    @Override
    public List<AdminRegisteredModel> findAllByEntityClass(Class<?> entityClass) {
        return modelsByEntityClass.getOrDefault(entityClass, List.of());
    }
}
