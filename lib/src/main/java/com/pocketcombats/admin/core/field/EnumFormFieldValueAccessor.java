package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.formatter.ValueFormatter;
import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import com.pocketcombats.admin.widget.Option;
import jakarta.annotation.Nullable;
import org.springframework.validation.BindingResult;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Enum form field accessor uses {@link Enum#ordinal()} to communicate selected values.
 */
public class EnumFormFieldValueAccessor extends AbstractFormFieldValueAccessor
        implements AdminFormFieldSingularValueAccessor {

    private final Enum<?>[] universe;
    private final List<Option> options;

    public EnumFormFieldValueAccessor(
            String name,
            Class<? extends Enum<?>> type,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer,
            ValueFormatter valueFormatter
    ) {
        super(name, reader, writer);

        this.universe = type.getEnumConstants();
        this.options = Arrays.stream(this.universe)
                .map(enumValue -> new Option(
                        String.valueOf(enumValue.ordinal()),
                        valueFormatter.format(enumValue)
                ))
                .toList();
    }

    @Override
    public String getDefaultTemplate() {
        return "admin/widget/to_one_choice";
    }

    @Override
    public String readValue(Object instance) {
        Enum<?> value = (Enum<?>) getReader().getValue(instance);
        return String.valueOf(value.ordinal());
    }

    @Override
    public void setValue(Object instance, @Nullable String value, BindingResult bindingResult) {
        int index = Integer.parseInt(value);
        Enum<?> resolvedValue = universe[index];
        getWriter().setValue(instance, resolvedValue);
    }

    @Override
    public Map<String, Object> getModelAttributes() {
        return Map.of("_options", options);
    }
}
