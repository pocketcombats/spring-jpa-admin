package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.BindingResult;

import java.util.Collections;
import java.util.Map;

public class DelegatingAdminFormFieldValueAccessorImpl extends AbstractFormFieldValueAccessor
        implements AdminFormFieldSingularValueAccessor {

    private static final Logger LOG = LoggerFactory.getLogger(DelegatingAdminFormFieldValueAccessorImpl.class);

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
    public @Nullable Object readValue(Object instance) {
        return getReader().getValue(instance);
    }

    @Override
    public void setValue(Object instance, @Nullable String value, BindingResult bindingResult) {
        AdminModelPropertyWriter writer = getWriter();

        Object convertedValue;
        try {
            convertedValue = conversionService.convert(value, writer.getJavaType());
        } catch (ConversionException e) {
            // Browsers can legitimately submit unconvertible text (e.g., a datetime-local value
            // has no seconds or offset); surface a field error instead of failing the request
            LOG.debug("Failed to convert value for field {}", getName(), e);
            bindingResult.rejectValue(getName(), "spring-jpa-admin.validation.constraints.ValidValue.message");
            return;
        }
        writer.setValue(instance, convertedValue);
    }

    @Override
    public Map<String, @Nullable Object> getModelAttributes(Object instance) {
        return Collections.emptyMap();
    }
}
