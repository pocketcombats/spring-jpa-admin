package com.pocketcombats.admin.core.search;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;

/* package */ final class PathUtils {

    private PathUtils() {
    }

    public static <T> Path<T> resolve(Root<?> root, String path) {
        final Path<T> attribute;
        if (path.contains(".")) {
            String[] parts = StringUtils.split(path, '.');
            Path<?> _target = root;
            for (int i = 0; i < parts.length - 1; i++) {
                _target = _target.get(parts[i]);
            }
            attribute = _target.get(parts[parts.length - 1]);
        } else {
            attribute = root.get(path);
        }
        return attribute;
    }
}
