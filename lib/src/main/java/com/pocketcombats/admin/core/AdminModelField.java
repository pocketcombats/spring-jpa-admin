package com.pocketcombats.admin.core;

import com.pocketcombats.admin.core.field.AdminFormFieldValueAccessor;
import jakarta.annotation.Nullable;

public record AdminModelField(
        String name,
        String label,
        @Nullable String description,
        String template,
        boolean insertable,
        boolean updatable,
        AdminFormFieldValueAccessor valueAccessor
) {

}
