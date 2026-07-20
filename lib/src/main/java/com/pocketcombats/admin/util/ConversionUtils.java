package com.pocketcombats.admin.util;

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;

public final class ConversionUtils {

    private ConversionUtils() {
    }

    /**
     * The value as the given target type, or {@code null} when it can't be represented — a
     * malformed or absent value (a tampered filter param, an unparseable submitted id) that the
     * caller should treat as "no match" / reject rather than let fail deeper.
     * <p>
     * A {@code null} input, and a value that converts to {@code null}, are both reported as
     * {@code null}; callers that must distinguish the two should not use this helper.
     */
    public static @Nullable Object tryConvert(
            ConversionService conversionService,
            @Nullable Object value,
            Class<?> targetType
    ) {
        if (value == null) {
            return null;
        }
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        try {
            return conversionService.convert(value, targetType);
        } catch (ConversionException | IllegalArgumentException e) {
            return null;
        }
    }
}
