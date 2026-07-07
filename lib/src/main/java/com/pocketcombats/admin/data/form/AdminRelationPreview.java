package com.pocketcombats.admin.data.form;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;

public record AdminRelationPreview(
        String entityId,
        @Nullable String representation
) implements Serializable {
}
