package com.pocketcombats.admin.data.form;

import java.io.Serializable;
import java.util.List;

public record AdminFormFieldGroup(
        String label,
        String cssClasses,
        List<AdminFormField> fields
) implements Serializable {

}
