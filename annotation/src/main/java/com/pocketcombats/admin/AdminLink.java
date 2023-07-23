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
     * Optional sort field.
     * May begin with "-" (minus) for descending order.
     */
    String sortBy() default "";
}
