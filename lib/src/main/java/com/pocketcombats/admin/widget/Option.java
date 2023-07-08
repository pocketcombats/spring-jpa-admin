package com.pocketcombats.admin.widget;

import java.io.Serializable;

public record Option(
        String id,
        String label,
        boolean localize
) implements Serializable {

    public static final Option EMPTY = new Option("-1", "spring-jpa-admin.choice.empty", true);

    public Option(String id, String label) {
        this(id, label, false);
    }
}
