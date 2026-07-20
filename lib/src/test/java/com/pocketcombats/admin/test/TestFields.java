package com.pocketcombats.admin.test;

import com.pocketcombats.admin.core.property.FieldPropertyReader;
import com.pocketcombats.admin.core.property.FieldPropertyWriter;

import java.lang.reflect.Field;

/**
 * Reflective field fixtures: a missing field is a broken test, surfaced as {@link AssertionError}.
 */
public final class TestFields {

    private TestFields() {
    }

    public static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    public static FieldPropertyReader reader(Class<?> owner, String name) {
        return new FieldPropertyReader(field(owner, name));
    }

    public static FieldPropertyWriter writer(Class<?> owner, String name) {
        return new FieldPropertyWriter(field(owner, name));
    }
}
