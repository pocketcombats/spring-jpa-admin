package com.pocketcombats.admin.core;

import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;

public record RegisteredEntityDetails(
        Class<?> entityClass,
        IdentifiableType<?> entityType,
        SingularAttribute<?, ?> idAttribute
) {

}
