package com.pocketcombats.admin.core.formatter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class SpelExpressionContextFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SpelExpressionContextFactory.class);

    private final Map<String, Method> functions = collectDefaultFunctions();

    public final StandardEvaluationContext createContext() {
        StandardEvaluationContext context = new StandardEvaluationContext();
        registerFunctions(context);
        return context;
    }

    protected void registerFunctions(StandardEvaluationContext context) {
        for (Map.Entry<String, Method> entry : functions.entrySet()) {
            context.registerFunction(entry.getKey(), entry.getValue());
        }
    }

    private static Map<String, Method> collectDefaultFunctions() {
        Map<String, Method> functions = new HashMap<>();

        Method abbreviate = BeanUtils.findMethod(StringUtils.class, "abbreviate", String.class, Integer.TYPE);
        if (abbreviate == null) {
            LOG.error("Unable to find StringUtils#abbreviate method");
        } else {
            functions.put("abbreviate", abbreviate);
        }

        return functions;
    }
}
