package com.pocketcombats.admin.data.form;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

public record AdminFormFieldGroup(
        @Nullable String label,
        @Nullable String cssClasses,
        List<AdminFormField> fields
) implements Serializable {

}
