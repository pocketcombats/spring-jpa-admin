package com.pocketcombats.admin.data.form;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Map;

public record AdminFormField(
        String name,
        String label,
        @Nullable String description,
        boolean readonly,
        String template,
        @Nullable Object value,
        Map<String, ?> templateData
) implements Serializable {

}
