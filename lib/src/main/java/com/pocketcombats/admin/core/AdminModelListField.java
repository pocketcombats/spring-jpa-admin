package com.pocketcombats.admin.core;

import com.pocketcombats.admin.core.property.AdminModelPropertyReader;

public record AdminModelListField(
        String name,
        String label,
        boolean sortable,
        AdminModelPropertyReader valueAccessor
) {

}
