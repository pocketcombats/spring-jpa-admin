package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import jakarta.annotation.Nullable;
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.BindingResult;

import java.util.Collections;
import java.util.Map;

public class DelegatingAdminFormFieldValueAccessorImpl extends AbstractFormFieldValueAccessor {

    private final ConversionService conversionService;

    public DelegatingAdminFormFieldValueAccessorImpl(
            String name,
            ConversionService conversionService,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer
    ) {
        super(name, reader, writer);
        this.conversionService = conversionService;
    }

    @Override
    public String getDefaultTemplate() {
        return "admin/widget/text";
    }

    @Override
    public Object readValue(Object instance) {
        return getReader().getValue(instance);
    }

    @Override
    public void setValue(Object instance, String value, BindingResult bindingResult) {
        AdminModelPropertyWriter writer = getWriter();

        Object convertedValue = conversionService.convert(value, writer.getJavaType());
        writer.setValue(instance, convertedValue);
    }

    @Override
    public Map<String, Object> getModelAttributes() {
        return Collections.emptyMap();
    }
}
