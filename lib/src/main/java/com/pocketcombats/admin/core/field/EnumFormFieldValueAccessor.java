package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.formatter.ValueFormatter;
import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import com.pocketcombats.admin.widget.Option;
import jakarta.annotation.Nullable;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
            boolean optional,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer,
            ValueFormatter valueFormatter
    ) {
        super(name, reader, writer);

        this.universe = type.getEnumConstants();
        this.options = constructOptions(this.universe, optional, valueFormatter);
    }

    private static List<Option> constructOptions(Enum<?>[] values, boolean optional, ValueFormatter valueFormatter) {
        List<Option> valueOptions = Arrays.stream(values)
                .map(enumValue -> new Option(
                        String.valueOf(enumValue.ordinal()),
                        valueFormatter.format(enumValue)
                ))
                .toList();
        if (optional) {
            List<Option> completeOptions = new ArrayList<>(valueOptions.size() + 1);
            completeOptions.add(Option.EMPTY);
            completeOptions.addAll(valueOptions);
            return Collections.unmodifiableList(completeOptions);
        } else {
            return valueOptions;
        }
    }

    @Override
    public String getDefaultTemplate() {
        return "admin/widget/select";
    }

    @Override
    public String readValue(Object instance) {
        Enum<?> value = (Enum<?>) getReader().getValue(instance);
        if (value == null) {
            return Option.EMPTY.id();
        } else {
            return String.valueOf(value.ordinal());
        }
    }

    @Override
    public void setValue(Object instance, String value, BindingResult bindingResult) {
        if (Option.EMPTY.id().equals(value)) {
            getWriter().setValue(instance, null);
        } else {
            int index = Integer.parseInt(value);
            Enum<?> resolvedValue = universe[index];
            getWriter().setValue(instance, resolvedValue);
        }
    }

    @Override
    public Map<String, Object> getModelAttributes() {
        return Map.of("_options", options);
    }
}
