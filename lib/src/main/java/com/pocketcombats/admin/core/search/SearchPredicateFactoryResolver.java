package com.pocketcombats.admin.core.search;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.UUID;

/**
 * Resolves {@code @AdminModel(searchFields = ...)} declarations into a {@link SearchPredicateFactory},
 * picking the factory from the attribute type at the end of each (possibly dotted) path.
 */
public class SearchPredicateFactoryResolver {

    private final ConversionService conversionService;

    public SearchPredicateFactoryResolver(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * Returns {@code null} when no search fields are declared, i.e., the model is not searchable.
     */
    public @Nullable SearchPredicateFactory resolve(String modelName, EntityType<?> entity, String... searchFields) {
        if (searchFields.length == 0) {
            return null;
        }
        if (searchFields.length == 1) {
            return resolveField(modelName, entity, searchFields[0]);
        }
        return new CompositeSearchPredicateFactory(
                Arrays.stream(searchFields)
                        .map(searchField -> resolveField(modelName, entity, searchField))
                        .toList()
        );
    }

    private SearchPredicateFactory resolveField(String modelName, EntityType<?> entity, String searchField) {
        return resolvePath(modelName, searchField, entity, searchField);
    }

    /**
     * {@code path} is relative to {@code type}; {@code searchField} is the full declared path,
     * kept for error messages.
     */
    private SearchPredicateFactory resolvePath(String modelName, String searchField, ManagedType<?> type, String path) {
        String[] parts = StringUtils.split(path, '.');
        ManagedType<?> current = type;
        for (int i = 0; i < parts.length - 1; i++) {
            Attribute<?, ?> segment = current.getAttribute(parts[i]);
            if (segment instanceof SingularAttribute<?, ?> singular
                    && singular.getType() instanceof ManagedType<?> managedType) {
                current = managedType;
            } else if (segment instanceof PluralAttribute<?, ?, ?> plural
                    && plural.getElementType() instanceof ManagedType<?> elementType) {
                // A to-many hop must not multiply result rows, so the rest of the path is matched
                // inside a correlated EXISTS; another plural in the remainder nests recursively.
                String collectionPath = String.join(".", Arrays.asList(parts).subList(0, i + 1));
                String remainder = String.join(".", Arrays.asList(parts).subList(i + 1, parts.length));
                return new ExistsSearchPredicateFactory(
                        collectionPath,
                        resolvePath(modelName, searchField, elementType, remainder)
                );
            } else {
                throw new IllegalStateException(
                        "Can't enable search for model " + modelName + ", field " + searchField +
                                ": segment \"" + parts[i] + "\" is not an association"
                );
            }
        }
        return matcher(modelName, searchField, path, current.getAttribute(parts[parts.length - 1]));
    }

    private SearchPredicateFactory matcher(String modelName, String searchField, String path, Attribute<?, ?> attribute) {
        Class<?> javaType = ClassUtils.resolvePrimitiveIfNecessary(attribute.getJavaType());
        if (Number.class.isAssignableFrom(javaType)) {
            //noinspection unchecked
            return new NumberSearchPredicateFactory(
                    path,
                    (Class<? extends Number>) javaType,
                    conversionService
            );
        } else if (CharSequence.class.isAssignableFrom(javaType)) {
            return new TextSearchPredicateFactory(path);
        } else if (UUID.class.isAssignableFrom(javaType)) {
            return new UUIDSearchPredicateFactory(path);
        } else {
            // Do we need to support search over boolean attributes?
            throw new IllegalStateException(
                    "Can't enable search for model " + modelName + ", field " + searchField +
                            ": unsupported type " + javaType
            );
        }
    }
}
