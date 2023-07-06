package com.pocketcombats.admin.data.list;

import java.io.Serializable;

public record ListFilterOption(
        String label,
        String value,
        boolean localize
) implements Serializable {
}
