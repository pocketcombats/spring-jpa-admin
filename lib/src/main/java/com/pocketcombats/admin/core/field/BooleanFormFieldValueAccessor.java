package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import org.springframework.validation.BindingResult;

import java.util.Collections;
import java.util.Map;

public class BooleanFormFieldValueAccessor extends AbstractFormFieldValueAccessor {

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
    public Object readValue(Object instance) {
        return getReader().getValue(instance);
    }

    @Override
    public void setValue(Object instance, @Nullable String value, BindingResult bindingResult) {
        if (value == null) {
            getWriter().setValue(instance, false);
        } else if ("true".equals(value)) {
            getWriter().setValue(instance, true);
        } else if ("false".equals(value)) {
            getWriter().setValue(instance, false);
        } else {
            throw new IllegalArgumentException("Unexpected boolean value: " + value);
        }
    }

    @Override
    public Map<String, Object> getModelAttributes(EntityManager em) {
        return Collections.emptyMap();
    }
}
