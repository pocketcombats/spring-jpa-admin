package com.pocketcombats.admin.core;

import com.pocketcombats.admin.ValueFormatter;
import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.sort.SortExpressionFactory;
import jakarta.annotation.Nullable;

public record AdminModelListField(
        String name,
        String label,
        AdminModelPropertyReader valueAccessor,
        @Nullable ValueFormatter valueFormatter,
        @Nullable SortExpressionFactory sortExpressionFactory
) {

    public boolean bool() {
        return valueAccessor.getJavaType().equals(Boolean.class) || valueAccessor.getJavaType().equals(Boolean.TYPE);
    }
}
