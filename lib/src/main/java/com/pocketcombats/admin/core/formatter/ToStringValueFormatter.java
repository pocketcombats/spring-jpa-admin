package com.pocketcombats.admin.core.formatter;

import org.jspecify.annotations.Nullable;
import org.springframework.lang.Contract;

public class ToStringValueFormatter implements ValueFormatter {

    @Override
    @Contract("!null -> !null")
    public @Nullable String format(@Nullable Object entity) {
        if (entity == null) {
            return null;
        } else {
            return entity.toString();
        }
    }
}
