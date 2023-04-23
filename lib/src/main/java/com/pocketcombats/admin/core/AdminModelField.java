package com.pocketcombats.admin.core;

import com.pocketcombats.admin.core.field.AdminFormFieldValueAccessor;

public record AdminModelField(
        String name,
        String label,
        String template,
        boolean insertable,
        boolean updatable,
        AdminFormFieldValueAccessor valueAccessor
) {

}
