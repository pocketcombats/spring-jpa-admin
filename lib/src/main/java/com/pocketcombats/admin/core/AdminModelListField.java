package com.pocketcombats.admin.core;

import com.pocketcombats.admin.ValueFormatter;
import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import jakarta.annotation.Nullable;

public record AdminModelListField(
        String name,
        String label,
        boolean sortable,
        AdminModelPropertyReader valueAccessor,
        @Nullable ValueFormatter valueFormatter
) {

}
