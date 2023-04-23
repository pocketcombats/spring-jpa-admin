package com.pocketcombats.admin.data.form;

import java.io.Serializable;
import java.util.List;

public record EntityDetails(
        String modelName,
        String id,
        List<AdminFormFieldGroup> fieldGroups
) implements Serializable {

}
