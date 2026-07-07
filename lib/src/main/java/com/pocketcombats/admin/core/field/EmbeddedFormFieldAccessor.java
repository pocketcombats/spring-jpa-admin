package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Form field accessor for JPA {@code @Embedded} attributes.
 * <p>
 * Each persistent property of the embeddable type is exposed as a separate input, submitted under the
 * {@code <field parameter>.<property>} request parameter name. The embeddable instance is created lazily
 * when the first non-empty property value is bound.
 */
public class EmbeddedFormFieldAccessor extends AbstractFormFieldValueAccessor {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedFormFieldAccessor.class);

    private final ConversionService conversionService;
    private final Class<?> embeddableType;
    private final List<EmbeddedFormFieldProperty> properties;
    private final Map<String, Object> modelAttributes;

    public EmbeddedFormFieldAccessor(
            ConversionService conversionService,
            String name,
            Class<?> embeddableType,
            List<EmbeddedFormFieldProperty> properties,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer
    ) {
        super(name, reader, writer);

        this.conversionService = conversionService;
        this.embeddableType = embeddableType;
        this.properties = List.copyOf(properties);
        this.modelAttributes = Map.of("_properties", this.properties);
    }

    @Override
    public String getDefaultTemplate() {
        return "admin/widget/embedded";
    }

    @Override
    public Map<String, Object> readValue(Object instance) {
        Object embeddable = getReader().getValue(instance);
        Map<String, Object> values = new LinkedHashMap<>(properties.size());
        for (EmbeddedFormFieldProperty property : properties) {
            values.put(property.getName(), embeddable == null ? null : property.read(embeddable));
        }
        return values;
    }

    @Override
    public Map<String, Object> getModelAttributes() {
        return modelAttributes;
    }

    /**
     * Binds submitted property values onto the embeddable held by {@code instance}.
     *
     * @param parameterName request parameter prefix of the embedded field (e.g. {@code model-field-address})
     */
    public void bind(
            String parameterName,
            Object instance,
            MultiValueMap<String, String> rawData,
            BindingResult bindingResult
    ) {
        Object embeddable = getReader().getValue(instance);
        if (embeddable == null) {
            // Instantiate lazily: keep the embeddable null unless at least one property was submitted with a
            // non-empty value, so entities without embedded data don't get an empty instance persisted.
            if (!isWritable() || !anyValueSubmitted(parameterName, rawData)) {
                return;
            }
            embeddable = BeanUtils.instantiateClass(embeddableType);
            getWriter().setValue(instance, embeddable);
        }
        for (EmbeddedFormFieldProperty property : properties) {
            if (!property.isWritable()) {
                continue;
            }
            String rawValue = rawData.getFirst(parameterName + "." + property.getName());
            try {
                Object converted = StringUtils.hasText(rawValue)
                        ? conversionService.convert(rawValue, property.getJavaType())
                        : null;
                property.write(embeddable, converted);
            } catch (ConversionException e) {
                LOG.debug("Failed to convert value for {}.{}", embeddableType.getSimpleName(), property.getName(), e);
                bindingResult.rejectValue(
                        getName() + "." + property.getName(),
                        "typeMismatch",
                        "Invalid value"
                );
            }
        }
    }

    private boolean anyValueSubmitted(String parameterName, MultiValueMap<String, String> rawData) {
        return properties.stream()
                .filter(EmbeddedFormFieldProperty::isWritable)
                .anyMatch(property -> StringUtils.hasText(
                        rawData.getFirst(parameterName + "." + property.getName())
                ));
    }
}
