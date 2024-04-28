package com.pocketcombats.admin;

public @interface AdminLink {

    /**
     * Optional custom label.
     * Will be picked up from registered admin model if no custom label provided.
     */
    String label() default "";

    /**
     * Target entity.
     * Must be a registered model admin.
     */
    Class<?> target();

    /**
     * The field that owns the relationship on the {@link #target()} class' side.
     */
    String mappedBy() default "";

    /**
     * Number of entities to preview directly in the "links" block on the form page.
     */
    int preview() default 0;

    /**
     * Representation formatter to for this relation in {@code SpEL} format.
     * If representation is not set, first entry from the {@code listFields} of the target model will be used.
     */
    String representation() default "";

    /**
     * Optional sort field.
     * May begin with "-" (minus) for descending order.
     */
    String sortBy() default "";
}
