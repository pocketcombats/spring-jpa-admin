package com.pocketcombats.admin.core.formatter;

import jakarta.annotation.Nullable;

public class ToStringValueFormatter implements ValueFormatter {

    @Override
    @Nullable
    public String format(@Nullable Object entity) {
        if (entity == null) {
            return null;
        } else {
            return entity.toString();
        }
    }
}
