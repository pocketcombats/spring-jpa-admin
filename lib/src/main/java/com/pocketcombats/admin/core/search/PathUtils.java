package com.pocketcombats.admin.core.search;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * Resolves dotted attribute paths against a query root or join.
 * Association segments are navigated with left joins (reusing compatible existing joins),
 * so rows with {@code null} relations are not silently dropped from results.
 */
public final class PathUtils {

    private PathUtils() {
    }

    public static <T> Path<T> resolve(From<?, ?> from, String path) {
        if (!path.contains(".")) {
            return from.get(path);
        }
        String[] parts = StringUtils.split(path, '.');
        Path<?> target = from;
        ManagedType<?> type = managedType(from);
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Attribute<?, ?> attribute = type == null ? null : type.getAttribute(part);
            if (attribute != null && attribute.isAssociation() && target instanceof From<?, ?> targetFrom) {
                target = leftJoin(targetFrom, part);
            } else {
                target = target.get(part);
            }
            type = targetManagedType(attribute);
        }
        return target.get(parts[parts.length - 1]);
    }

    private static From<?, ?> leftJoin(From<?, ?> from, String attributeName) {
        for (Join<?, ?> join : from.getJoins()) {
            if (join.getJoinType() == JoinType.LEFT
                    && join.getAttribute().getName().equals(attributeName)
                    && join.getOn() == null) {
                return join;
            }
        }
        return from.join(attributeName, JoinType.LEFT);
    }

    private static @Nullable ManagedType<?> managedType(From<?, ?> from) {
        if (from instanceof Root<?> root) {
            return root.getModel();
        }
        if (from instanceof Join<?, ?> join) {
            return targetManagedType(join.getAttribute());
        }
        return null;
    }

    private static @Nullable ManagedType<?> targetManagedType(@Nullable Attribute<?, ?> attribute) {
        Type<?> targetType;
        if (attribute instanceof SingularAttribute<?, ?> singular) {
            targetType = singular.getType();
        } else if (attribute instanceof PluralAttribute<?, ?, ?> plural) {
            targetType = plural.getElementType();
        } else {
            return null;
        }
        return targetType instanceof ManagedType<?> managedType ? managedType : null;
    }
}
