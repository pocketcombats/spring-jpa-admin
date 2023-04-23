package com.pocketcombats.admin.util;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class AdminStringUtils {

    private static final Pattern LIKE_ESCAPE_PATTERN = Pattern.compile("([%_\\]\\[\\\\])");
    private static final Pattern HUMAN_READABLE_PATTERN = Pattern.compile(
            "((?<=[a-z])(?=[A-Z]))|[_\\-\\s]", Pattern.UNICODE_CHARACTER_CLASS);

    private AdminStringUtils() {

    }

    public static String escapeLikeClause(String input) {
        return LIKE_ESCAPE_PATTERN.matcher(input).replaceAll("\\$1");
    }

    public static String toHumanReadableName(String input) {
        return HUMAN_READABLE_PATTERN.splitAsStream(input)
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));
    }
}
