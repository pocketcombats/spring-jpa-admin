package com.pocketcombats.admin.thymeleaf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AdminUrlParamsHelperTest {

    private final AdminUrlParamsHelper helper = new AdminUrlParamsHelper();

    @Test
    void preservesSearchAndFilterParamsAlongsideNewParam() {
        Map<String, Object> requestParams = new LinkedHashMap<>();
        requestParams.put("search", new String[]{"abc"});
        requestParams.put("filter:enabled", List.of("true"));
        requestParams.put("page", new String[]{"3"});
        requestParams.put("unrelated", new String[]{"x"});

        assertEquals(
                "?search=abc&filter:enabled=true&sort=-name",
                helper.queryString(requestParams, "sort", "-name")
        );
    }

    @Test
    void replacesExistingValueOfNamedParam() {
        Map<String, Object> requestParams = Map.of("sort", new String[]{"name"});

        assertEquals("?sort=-name", helper.queryString(requestParams, "sort", "-name"));
    }

    @Test
    void emptyValueOmitsNamedParam() {
        Map<String, Object> requestParams = new LinkedHashMap<>();
        requestParams.put("filter:enabled", new String[]{"true"});
        requestParams.put("search", new String[]{"q"});

        assertEquals("?search=q", helper.queryString(requestParams, "filter:enabled", ""));
    }

    @Test
    void returnsEmptyStringWhenThereAreNoParams() {
        assertEquals("", helper.queryString(Map.of(), "search", ""));
    }

    @Test
    void returnsEmptyStringWhenRemovingTheOnlyParam() {
        assertEquals(
                "",
                helper.queryString(Map.of("filter:enabled", new String[]{"true"}), "filter:enabled", "")
        );
    }

    @Test
    void currentPageIsNotCarriedOver() {
        // Changing sort (or search/filter) must reset pagination
        Map<String, Object> requestParams = new LinkedHashMap<>();
        requestParams.put("page", new String[]{"7"});
        requestParams.put("search", new String[]{"q"});

        assertEquals("?search=q&sort=name", helper.queryString(requestParams, "sort", "name"));
    }

    @Test
    void acceptsNonStringValues() {
        // Pagination links pass page numbers as integers
        assertEquals("?page=2", helper.queryString(Map.of(), "page", 2));
    }

    @ParameterizedTest
    @MethodSource("multiValuedRepresentations")
    void takesFirstValueOfMultiValuedParams(Object multiValued) {
        assertEquals("?search=first", helper.queryString(Map.of("search", multiValued), "sort", ""));
    }

    static Stream<Arguments> multiValuedRepresentations() {
        return Stream.of(
                arguments((Object) new String[]{"first", "second"}),
                arguments(List.of("first", "second"))
        );
    }

    @Test
    void acceptsPlainStringParamValues() {
        assertEquals(
                "?search=plain",
                helper.queryString(Map.of("search", "plain"), "sort", "")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "two words",
            "a&b=c",
            "\"quoted\" 'text'",
            "ünïcødé 検索",
            "1+1",
            "${T(java.lang.Runtime).getRuntime().exec('id')}",
            "__${T(java.lang.Runtime)}__",
    })
    void hostileValuesRoundTripAsInertEncodedData(String value) {
        String query = helper.queryString(Map.of(), "search", value);

        assertTrue(query.startsWith("?search="));
        String encoded = query.substring("?search=".length());
        // Characters that could terminate the parameter, break out of the attribute,
        // or feed Thymeleaf expression preprocessing must be percent-encoded
        assertFalse(encoded.contains(" "));
        assertFalse(encoded.contains("&"));
        assertFalse(encoded.contains("="));
        assertFalse(encoded.contains("\""));
        assertFalse(encoded.contains("$"));
        assertFalse(encoded.contains("{"));
        assertFalse(encoded.contains("}"));
        // A literal '+' would be decoded as a space by servlet query string parsing
        assertFalse(encoded.contains("+"));
        assertEquals(value, UriUtils.decode(encoded, StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "two words",
            "a&b=c",
            "${T(java.lang.Runtime).getRuntime().exec('id')}",
    })
    void hostileValuesArePreservedAcrossLinks(String value) {
        // A hostile search value carried over into a sort link stays inert and intact
        Map<String, Object> requestParams = Map.of("search", new String[]{value});

        String query = helper.queryString(requestParams, "sort", "name");

        assertTrue(query.endsWith("&sort=name"));
        assertFalse(query.contains("${"));
        String encoded = query.substring("?search=".length(), query.indexOf("&sort=name"));
        assertEquals(value, UriUtils.decode(encoded, StandardCharsets.UTF_8));
    }
}
