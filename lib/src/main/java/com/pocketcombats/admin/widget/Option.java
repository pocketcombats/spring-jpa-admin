package com.pocketcombats.admin.widget;

import java.io.Serializable;

public record Option(
        String id,
        String label,
        boolean localize
) implements Serializable {

    public Option(String id, String label) {
        this(id, label, false);
    }
}
