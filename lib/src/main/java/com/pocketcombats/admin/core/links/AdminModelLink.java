package com.pocketcombats.admin.core.links;

import com.pocketcombats.admin.core.formatter.ValueFormatter;
import jakarta.annotation.Nullable;

public record AdminModelLink(
        @Nullable String label,
        Class<?> target,
        LinkPredicateFactory predicateFactory,
        int preview,
        @Nullable ValueFormatter formatter,
        @Nullable String order,
        @Nullable LinkOrderFactory orderFactory
) {
}
