package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import jakarta.annotation.Nullable;

/**
 * Describes a single property of a JPA {@code @Embeddable} type exposed as a nested form field.
 *
 * @see EmbeddedFormFieldAccessor
 */
public class EmbeddedFormFieldProperty {

    private final String name;
    private final String label;
    private final Class<?> javaType;
    private final AdminModelPropertyReader reader;
    private final @Nullable AdminModelPropertyWriter writer;

    public EmbeddedFormFieldProperty(
            String name,
            String label,
            Class<?> javaType,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer
    ) {
        this.name = name;
        this.label = label;
        this.javaType = javaType;
        this.reader = reader;
        this.writer = writer;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public boolean isWritable() {
        return writer != null;
    }

    public Object read(Object embeddable) {
        return reader.getValue(embeddable);
    }

    public void write(Object embeddable, @Nullable Object value) {
        if (writer == null) {
            throw new IllegalStateException("Embedded property " + name + " is not writable");
        }
        writer.setValue(embeddable, value);
    }
}
