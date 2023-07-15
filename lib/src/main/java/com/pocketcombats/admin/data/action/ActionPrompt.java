package com.pocketcombats.admin.data.action;

import com.pocketcombats.admin.data.list.AdminEntityListEntry;

import java.io.Serializable;
import java.util.List;

public record ActionPrompt(
        String action,
        String label,
        String description,
        String modelName,
        String modelLabel,
        List<ActionColumn> columns,
        List<AdminEntityListEntry> entities
) implements Serializable {

}
