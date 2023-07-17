package com.pocketcombats.admin.core.formatter;

import jakarta.annotation.Nullable;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class SpelExpressionFormatter implements ValueFormatter {

    private final SpelExpressionContextFactory contextFactory;
    private final Expression expression;

    public SpelExpressionFormatter(
            SpelExpressionContextFactory contextFactory,
            String spelExpression
    ) throws ParseException {
        this.contextFactory = contextFactory;
        this.expression = new SpelExpressionParser().parseExpression(spelExpression);
    }

    @Override
    @Nullable
    public String format(@Nullable Object entity) {
        Object value = expression.getValue(contextFactory.createContext(), entity);
        if (value == null) {
            return null;
        } else {
            return value.toString();
        }
    }
}
