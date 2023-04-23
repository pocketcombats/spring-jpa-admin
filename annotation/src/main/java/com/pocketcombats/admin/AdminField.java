package com.pocketcombats.admin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminField {

    String label() default "";

    /**
     * Override the default display value for entity’s fields that are empty (null, empty string, etc.).
     * The default value is — (a dash).
     */
    String emptyValue() default "—";

    /**
     * Set to {@code false} to force model field to be non-insertable.
     */
    boolean insertable() default true;

    /**
     * Set to {@code false} to force model field to be non-updatable.
     */
    boolean updatable() default true;

    /**
     * Custom for this field.
     */
    String template() default "";
}
