package com.pocketcombats.admin.core.sort;

import com.pocketcombats.admin.core.search.PathUtils;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

public class PathSortExpressionFactory implements SortExpressionFactory {

    private final String path;

    public PathSortExpressionFactory(String path) {
        this.path = path;
    }

    @Override
    public Expression<?> createExpression(Root<?> root) {
        return PathUtils.resolve(root, path);
    }
}
