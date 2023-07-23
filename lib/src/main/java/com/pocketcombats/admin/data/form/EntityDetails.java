package com.pocketcombats.admin.data.form;

import java.io.Serializable;
import java.util.List;

public record EntityDetails(
        String modelName,
        String id,
        String label,
        List<AdminFormFieldGroup> fieldGroups,
        List<AdminRelationLink> links,
        boolean deletable
) implements Serializable {

}
