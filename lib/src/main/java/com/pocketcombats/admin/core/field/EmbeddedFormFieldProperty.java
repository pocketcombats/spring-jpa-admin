package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import org.jspecify.annotations.Nullable;

/**
 * Describes a single property of a JPA {@code @Embeddable} type exposed as a nested form field.
 *
 * @see EmbeddedFormFieldAccessor
 */
public record EmbeddedFormFieldProperty(
        String name,
        String label,
        Class<?> javaType,
        AdminModelPropertyReader reader,
        @Nullable AdminModelPropertyWriter writer
) {

    public boolean isWritable() {
        return writer != null;
    }

    public @Nullable Object read(Object embeddable) {
        return reader.getValue(embeddable);
    }

    public void write(Object embeddable, @Nullable Object value) {
        if (writer == null) {
            throw new IllegalStateException("Embedded property " + name + " is not writable");
        }
        writer.setValue(embeddable, value);
    }
}
