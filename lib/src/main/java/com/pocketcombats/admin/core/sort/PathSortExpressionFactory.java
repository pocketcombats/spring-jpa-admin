package com.pocketcombats.admin.core.sort;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;

public class PathSortExpressionFactory implements SortExpressionFactory {

    private final String[] pathTokens;

    public PathSortExpressionFactory(String path) {
        this.pathTokens = StringUtils.split(path, '.');
    }

    @Override
    public Expression<?> createExpression(Root<?> root) {
        Path<?> path = root;
        for (String token : pathTokens) {
            path = path.get(token);
        }
        return path;
    }
}
