package com.pocketcombats.admin.core.links;

import jakarta.annotation.Nullable;

public record AdminModelLink(
        @Nullable String label,
        Class<?> target,
        LinkPredicateFactory predicateFactory,
        int preview,
        @Nullable String order,
        @Nullable LinkOrderFactory orderFactory
) {
}
