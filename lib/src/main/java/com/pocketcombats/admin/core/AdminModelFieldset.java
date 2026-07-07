package com.pocketcombats.admin.core;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record AdminModelFieldset(
        @Nullable String label,
        List<AdminModelField> fields,
        boolean unique
) {

}
