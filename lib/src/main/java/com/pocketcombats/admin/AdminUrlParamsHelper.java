package com.pocketcombats.admin;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.WebEngineContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AdminUrlParamsHelper {

    private static final Set<String> EXPECTED_PARAMS = Set.of("sort", "search");

    public String join(Map<String, Object> param, String name, String value) {
        Map<String, String> urlParams = new HashMap<>();
        for (var entry : param.entrySet()) {
            String paramKey = entry.getKey();
            if (paramKey.startsWith("filter:") || EXPECTED_PARAMS.contains(paramKey)) {
                if (name.equals(paramKey)) {
                    urlParams.put(name, value);
                } else {
                    var paramValues = (WebEngineContext.RequestParameterValues) entry.getValue();
                    if (!paramValues.isEmpty()) {
                        urlParams.put(paramKey, paramValues.get(0));
                    }
                }
            }
        }
        if (value.equals("")) {
            urlParams.remove(name);
        } else {
            urlParams.put(name, value);
        }
        return urlParams.entrySet().stream()
                .map(e -> "'" + e.getKey() + "'=" + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
