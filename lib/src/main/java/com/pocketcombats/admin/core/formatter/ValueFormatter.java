package com.pocketcombats.admin.core.formatter;

import org.jspecify.annotations.Nullable;
import org.springframework.lang.Contract;

@FunctionalInterface
public interface ValueFormatter {

    @Contract("!null -> !null")
    @Nullable String format(@Nullable Object entity);
}
