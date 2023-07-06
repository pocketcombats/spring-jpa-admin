package com.pocketcombats.admin.data.list;

import java.io.Serializable;
import java.util.List;

public record ListFilter(
        String name,
        String label,
        List<ListFilterOption> options
) implements Serializable {
}
