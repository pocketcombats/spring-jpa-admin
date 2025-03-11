package com.pocketcombats.admin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fieldsets are named collections of form fields, enabling the grouping of related fields.
 * This allows for improved appearance and organization of the edit form.
 * Custom labels can be provided for each fieldset to further enhance the form's view.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminFieldset {

    String label() default "";

    /**
     * Defines the field names to be included in this fieldset.
     * <p>
     * Each value in the array must correspond to a valid field name in the
     * annotated entity. Fields will be displayed in the order specified in this array.
     */
    String[] fields();

    /**
     * Indicates whether the combination of listed fields must be unique.
     * <p>
     * When set to {@code true}, the system will verify that no other entity has the same
     * combination of values for these fields before creating or updating an entity.
     * <p>
     * For example, if a fieldset contains "username" and "domain" with {@code unique=true},
     * the system will ensure that no two entities can have the same combination of username and domain.
     * <p>
     * For single field uniqueness validation you can rely on {@code Column#unique}.
     */
    boolean unique() default false;
}
