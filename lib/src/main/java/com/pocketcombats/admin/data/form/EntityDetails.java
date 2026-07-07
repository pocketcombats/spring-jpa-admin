package com.pocketcombats.admin.data.form;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

public record EntityDetails(
        String modelName,
        @Nullable String id,
        String label,
        List<AdminFormFieldGroup> fieldGroups,
        List<AdminRelationLink> links,
        boolean deletable
) implements Serializable {

}
