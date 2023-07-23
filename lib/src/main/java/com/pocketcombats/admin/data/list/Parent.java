package com.pocketcombats.admin.data.list;

import com.pocketcombats.admin.data.form.AdminRelationPreview;

import java.io.Serializable;

public record Parent(
        String label,
        String modelName,
        AdminRelationPreview entity
) implements Serializable {
}
