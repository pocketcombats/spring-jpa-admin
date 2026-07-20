package com.pocketcombats.admin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate admin model or entity method to create custom admin bulk action.
 * Annotated method must accept single argument of type {@code List<EntityType>}.
 * Annotated entity method must be declared static;
 * in Kotlin, declare entity actions in the entity's companion object.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminAction {

    /**
     * Custom action name. Defaults to annotated method name.
     */
    String label() default "";

    /**
     * Custom confirmation message.
     */
    String description() default "";
}
