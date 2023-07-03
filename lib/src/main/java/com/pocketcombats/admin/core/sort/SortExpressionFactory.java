package com.pocketcombats.admin.core.sort;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

@FunctionalInterface
public interface SortExpressionFactory {

    Expression<?> createExpression(Root<?> root);
}
