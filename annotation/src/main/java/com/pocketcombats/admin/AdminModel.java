package com.pocketcombats.admin;

import org.springframework.stereotype.Indexed;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to define an admin model for the JPA Admin interface.
 * <p>
 * If applied to an entity class, it will directly use that class as the model entity.
 * If applied to a non-entity class, the {@link #entity()} attribute must be specified
 * to indicate which entity class this admin model represents.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface AdminModel {

    /**
     * Target entity.
     * Has no effect if {@code @AdminModel} is put directly on entity.
     * Otherwise, a valid entity class must be provided.
     */
    Class<?> entity() default Void.class;

    /**
     * Registry name of this model, used for identification in admin URLs.
     * Defaults to the entity's simple class name; set explicitly to disambiguate
     * entities that share a simple name.
     */
    String name() default "";

    /**
     * Defines permission requirements for different actions on this admin model.
     * <p>
     * See {@link AdminModelPermissions} for more details on how to configure permissions.
     */
    AdminModelPermissions permissions() default @AdminModelPermissions;

    String label() default "";

    String[] listFields() default {};

    int pageSize() default 20;

    /**
     * Attributes matched by the list view search box. Entries may be dotted paths through
     * relations, including to-many ones: {@code "posts.title"} matches entities containing
     * at least one such element, without duplicating result rows.
     */
    String[] searchFields() default {};

    String[] filterFields() default {};

    /**
     * Default order for the list view.
     * Specified field must be {@link AdminField#sortable()}.
     * May begin with "-" (minus) for descending order.
     */
    String defaultOrder() default "";

    AdminFieldset[] fieldsets() default {};

    AdminLink[] links() default {};

    boolean insertable() default true;

    boolean updatable() default true;

    String[] disableActions() default {};

    AdminFieldOverride[] fieldOverrides() default {};
}
