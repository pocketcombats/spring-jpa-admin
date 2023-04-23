package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import jakarta.annotation.Nullable;

public abstract class AbstractFormFieldValueAccessor implements AdminFormFieldValueAccessor {

    private final String name;
    private final AdminModelPropertyReader reader;
    @Nullable
    private final AdminModelPropertyWriter writer;

    public AbstractFormFieldValueAccessor(
            String name,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer
    ) {
        this.name = name;
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<?> getReaderJavaType() {
        return reader.getJavaType();
    }

    @Override
    public boolean isWritable() {
        return writer != null;
    }

    @Override
    public Class<?> getWriterJavaType() {
        return writer == null ? Void.TYPE : writer.getJavaType();
    }

    protected AdminModelPropertyReader getReader() {
        return reader;
    }

    protected AdminModelPropertyWriter getWriter() {
        if (writer == null) {
            throw new IllegalStateException("No writer set for field " + getName());
        }
        return writer;
    }
}
