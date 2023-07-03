package com.pocketcombats.admin.core.formatter;

import jakarta.annotation.Nullable;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class SpelExpressionFormatter implements ValueFormatter {

    private final Expression expression;

    public SpelExpressionFormatter(String spelExpression) {
        this.expression = new SpelExpressionParser().parseExpression(spelExpression);
    }

    @Override
    @Nullable
    public String format(@Nullable Object entity) {
        Object value = expression.getValue(entity);
        if (value == null) {
            return null;
        } else {
            return value.toString();
        }
    }
}
