package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.formatter.ValueFormatter;
import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import com.pocketcombats.admin.widget.Option;
import org.jspecify.annotations.Nullable;
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
    private final boolean optional;
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
        this.optional = optional;
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
    public void setValue(Object instance, @Nullable String value, BindingResult bindingResult) {
        if (Option.EMPTY.id().equals(value)) {
            if (optional) {
                getWriter().setValue(instance, null);
            } else {
                // A required field's select never offers the empty option; a submitted sentinel is a
                // stale or hand-crafted form and must not write null into a non-optional attribute.
                bindingResult.rejectValue(getName(), "jakarta.validation.constraints.NotNull.message");
            }
            return;
        }
        // A missing parameter, non-numeric text, or an ordinal outside the universe (a stale form
        // submitted after the enum changed) must yield a field error, not a server error
        int ordinal = -1;
        if (value != null) {
            try {
                ordinal = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Falls through to the range check below
            }
        }
        if (ordinal < 0 || ordinal >= universe.length) {
            bindingResult.rejectValue(getName(), "spring-jpa-admin.validation.constraints.ValidValue.message");
            return;
        }
        getWriter().setValue(instance, universe[ordinal]);
    }

    @Override
    public Map<String, Object> getModelAttributes(Object instance) {
        return Map.of("_options", options);
    }
}
