package com.pocketcombats.admin.data.form;

import java.io.Serializable;
import java.util.Map;

public record AdminFormField(
        String name,
        String label,
        boolean readonly,
        String template,
        Object value,
        Map<String, ?> templateData
) implements Serializable {

}
