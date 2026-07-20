package com.pocketcombats.admin.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AdminModelRegistry {

    AdminRegisteredModel resolve(String modelName) throws UnknownModelException;

    /**
     * Finds the primary admin model registered for the given entity class. Unlike {@link #resolve},
     * absence is a normal outcome: not every entity is registered with the admin site. When the same
     * entity class is registered as several admin models, the registration named after the entity's
     * simple class name is the primary; between custom-named registrations, the lexicographically
     * smallest model name wins.
     */
    Optional<AdminRegisteredModel> findByEntityClass(Class<?> entityClass);

    /**
     * All admin models registered for the given entity class, primary first (the same order-defining
     * rule as {@link #findByEntityClass}). Empty when the entity is not registered.
     */
    List<AdminRegisteredModel> findAllByEntityClass(Class<?> entityClass);

    Map<String, List<AdminRegisteredModel>> getCategorizedModels();
}
