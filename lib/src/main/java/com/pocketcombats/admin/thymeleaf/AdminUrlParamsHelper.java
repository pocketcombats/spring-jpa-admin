package com.pocketcombats.admin.thymeleaf;

import org.jspecify.annotations.Nullable;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds URL-encoded query strings for admin list page links.
 * <p>
 * Search, sort, and filter parameters of the current request are carried over, so that e.g.,
 * switching sort order keeps the active search and filters. The result is a finished query
 * string ({@code "?a=b&c=d"} or {@code ""}) meant to be appended to a URL as-is; request
 * parameter values are percent-encoded and must never be fed to expression preprocessing.
 */
public class AdminUrlParamsHelper {

    private static final Set<String> EXPECTED_PARAMS = Set.of("sort", "search");

    /**
     * Returns the query string for a link that sets {@code name} to {@code value} while
     * preserving the current search/sort/filter request parameters. An empty {@code value}
     * omits {@code name} instead.
     */
    public String queryString(Map<String, ?> requestParams, String name, @Nullable Object value) {
        Map<String, String> urlParams = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : requestParams.entrySet()) {
            String paramKey = entry.getKey();
            if ((paramKey.startsWith("filter:") || EXPECTED_PARAMS.contains(paramKey))
                    && !paramKey.equals(name)) {
                String paramValue = firstValue(entry.getValue());
                if (paramValue != null) {
                    urlParams.put(paramKey, paramValue);
                }
            }
        }
        String newValue = value == null ? "" : String.valueOf(value);
        if (!newValue.isEmpty()) {
            urlParams.put(name, newValue);
        }
        if (urlParams.isEmpty()) {
            return "";
        }
        return urlParams.entrySet().stream()
                .map(entry -> encodeKey(entry.getKey()) + "=" + encodeValue(entry.getValue()))
                .collect(Collectors.joining("&", "?", ""));
    }

    private static @Nullable String firstValue(@Nullable Object value) {
        if (value instanceof String string) {
            return string;
        }
        if (value instanceof String[] values) {
            return values.length > 0 ? values[0] : null;
        }
        if (value instanceof Collection<?> collection) {
            // Covers Thymeleaf's RequestParameterValues, which is a List<String>
            return collection.isEmpty() ? null : String.valueOf(collection.iterator().next());
        }
        return null;
    }

    private static String encodeKey(String key) {
        return UriUtils.encodeQueryParam(key, StandardCharsets.UTF_8);
    }

    private static String encodeValue(String value) {
        // Strict RFC 3986 encoding, not encodeQueryParam: the latter leaves '+' as-is,
        // which servlet containers decode back as a space, corrupting the value
        return UriUtils.encode(value, StandardCharsets.UTF_8);
    }
}
