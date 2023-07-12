package com.pocketcombats.admin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate admin model or entity method to create custom admin bulk action.
 * Annotated method must accept single argument of type {@code List<EntityType>}.
 * Annotated entity method must be declared static.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminAction {

    /**
     * Custom action name. Defaults to annotated method name.
     * Will be localized if {@link #localize()} is set to true.
     */
    String label() default "";

    /**
     * Custom confirmation message.
     * Will be localized if {@link #localize()} is set to true.
     */
    String description() default "";

    /**
     * Set to {@code true} if {@link #label()} and {@link #description()} should be localized.
     * In this case they must have explicit values set.
     */
    boolean localize() default false;
}
