package com.pocketcombats.admin.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AdminStringUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "ClassName,Class Name",
            "propertyName,Property Name",
            "property_name,Property Name",
            "ClassName_,Class Name",
    })
    public void testHumanReadableNameFormatter(String input, String expectedOutput) {
        assertEquals(expectedOutput, AdminStringUtils.toHumanReadableName(input));
    }

    @ParameterizedTest
    @CsvSource({
            "plain,plain",
            "50%,50\\%",
            "a_b,a\\_b",
            "50%_off,50\\%\\_off",
            "[test],\\[test\\]",
            "a\\b,a\\\\b",
    })
    public void escapesLikeWildcards(String input, String expectedOutput) {
        assertEquals(expectedOutput, AdminStringUtils.escapeLikeClause(input));
    }
}
