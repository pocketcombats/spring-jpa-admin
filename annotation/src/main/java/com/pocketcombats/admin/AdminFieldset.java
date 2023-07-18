package com.pocketcombats.admin;

/**
 * Fieldsets are named collections of form fields, enabling the grouping of related fields.
 * This allows for improved appearance and organization of the edit form.
 * Custom labels can be provided for each fieldset to further enhance the form's view.
 */
public @interface AdminFieldset {

    String label() default "";

    String[] fields();
}
