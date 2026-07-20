package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import org.jspecify.annotations.Nullable;
import org.springframework.validation.BindingResult;

import java.util.Collections;
import java.util.Map;

public class BooleanFormFieldValueAccessor extends AbstractFormFieldValueAccessor
        implements AdminFormFieldSingularValueAccessor {

    public BooleanFormFieldValueAccessor(
            String name,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer
    ) {
        super(name, reader, writer);
    }

    @Override
    public String getDefaultTemplate() {
        return "admin/widget/checkbox";
    }

    @Override
    public @Nullable Object readValue(Object instance) {
        return getReader().getValue(instance);
    }

    @Override
    public void setValue(Object instance, @Nullable String value, BindingResult bindingResult) {
        if (value == null) {
            // Unchecked checkboxes are not submitted at all
            getWriter().setValue(instance, false);
        } else if ("true".equals(value)) {
            getWriter().setValue(instance, true);
        } else if ("false".equals(value)) {
            getWriter().setValue(instance, false);
        } else {
            bindingResult.rejectValue(getName(), "spring-jpa-admin.validation.constraints.ValidValue.message");
        }
    }

    @Override
    public Map<String, @Nullable Object> getModelAttributes(Object instance) {
        return Collections.emptyMap();
    }
}
