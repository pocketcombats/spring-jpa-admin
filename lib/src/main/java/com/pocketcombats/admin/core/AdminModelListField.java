package com.pocketcombats.admin.core;

import com.pocketcombats.admin.core.formatter.ValueFormatter;
import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.sort.SortExpressionFactory;
import com.pocketcombats.admin.util.TypeUtils;
import org.jspecify.annotations.Nullable;

public record AdminModelListField(
        String name,
        String label,
        String emptyValue,
        AdminModelPropertyReader valueAccessor,
        @Nullable ValueFormatter valueFormatter,
        @Nullable SortExpressionFactory sortExpressionFactory
) {

    public boolean bool() {
        return TypeUtils.isBoolean(valueAccessor.getJavaType());
    }
}
