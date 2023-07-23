package com.pocketcombats.admin.data.form;

import java.io.Serializable;

public record AdminRelationPreview(
        String entityId,
        String representation
) implements Serializable {
}
