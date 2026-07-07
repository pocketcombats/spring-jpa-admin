package com.pocketcombats.admin.widget;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;

public record Option(
        String id,
        @Nullable String label,
        boolean localize
) implements Serializable {

    public static final Option EMPTY = new Option("-1", "spring-jpa-admin.choice.empty", true);

    public Option(String id, @Nullable String label) {
        this(id, label, false);
    }
}
