package com.pocketcombats.admin.data.form;

import java.io.Serializable;
import java.util.List;

public record AdminRelationLink(
        String label,
        String modelName,
        List<AdminRelationPreview> entities
) implements Serializable {
}
