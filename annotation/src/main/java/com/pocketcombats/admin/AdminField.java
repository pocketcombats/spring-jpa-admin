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

    /**
     * Custom label for this field.
     * Can be a localization key or plain text.
     */
    String label() default "";

    /**
     * Form field description.
     * Can be a localization key or plain text.
     */
    String description() default "";

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
     * Set to {@code false} to force model field to be not null.
     */
    boolean nullable() default true;

    /**
     * If field should be a raw id input instead of select.
     */
    boolean rawId() default false;

    /**
     * Indicates that field is sortable.
     * Only entity fields included in {@link AdminModel#listFields()} can be sortable.
     */
    boolean sortable() default false;

    /**
     * Optional custom path to sort this field by.
     * Implicitly enables {@link #sortable()}.
     */
    String sortBy() default "";

    /**
     * Custom template for this field.
     */
    String template() default "";

    /**
     * Representation formatter to for this field in {@code SpEL} format.
     * Typically used by list view and relational form fields.
     * Defaults to calling {@code toString()}.
     *
     * @see <a href="https://docs.spring.io/spring-framework/docs/3.0.x/reference/expressions.html">SpEL documentation</a>
     */
    String representation() default "";
}
