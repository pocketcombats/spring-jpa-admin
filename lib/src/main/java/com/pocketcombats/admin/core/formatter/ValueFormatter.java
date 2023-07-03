package com.pocketcombats.admin.core.formatter;

import jakarta.annotation.Nullable;

@FunctionalInterface
public interface ValueFormatter {

    @Nullable
    String format(@Nullable Object entity);
}
