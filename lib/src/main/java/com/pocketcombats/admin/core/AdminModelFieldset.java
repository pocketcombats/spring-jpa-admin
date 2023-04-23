package com.pocketcombats.admin.core;

import jakarta.annotation.Nullable;

import java.util.List;

public record AdminModelFieldset(
        @Nullable String label,
        List<AdminModelField> fields
) {

}
