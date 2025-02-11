package com.pocketcombats.admin.data.form;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Map;

public record AdminFormField(
        String name,
        String label,
        @Nullable String description,
        boolean readonly,
        String template,
        Object value,
        Map<String, ?> templateData
) implements Serializable {

}
