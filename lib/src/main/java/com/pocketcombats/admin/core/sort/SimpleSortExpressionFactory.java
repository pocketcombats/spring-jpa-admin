package com.pocketcombats.admin.core.sort;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

public class SimpleSortExpressionFactory implements SortExpressionFactory {

    private final String attributeName;

    public SimpleSortExpressionFactory(String attributeName) {
        this.attributeName = attributeName;
    }

    @Override
    public Expression<?> createExpression(Root<?> root) {
        return root.get(attributeName);
    }
}
