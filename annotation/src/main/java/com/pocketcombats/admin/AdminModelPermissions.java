package com.pocketcombats.admin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define permission requirements for different actions on an admin model.
 * This annotation is used within {@link AdminModel} to specify which permissions are required
 * to view, edit, or create entities of the model.
 * <p>
 * Each action (view, edit, create) can require one or more permissions. If no permissions are specified
 * for an action, it means that action is available to all users who have access to the admin interface.
 * If permissions are specified, the user must have at least one of the listed permissions to perform the action.
 * <p>
 * The permissions are checked against the authorities of the current user using Spring Security.
 * The permission names should match the authority names granted to users in your security configuration.
 * <p>
 * Example usage:
 * <pre>
 * &#64;AdminModel(
 *     permissions = &#64;AdminModelPermissions(
 *         view = {},
 *         edit = {"ROLE_MANAGER", "ROLE_ADMIN"},
 *         create = {"ROLE_ADMIN"}
 *     )
 * )
 * public class Post {
 *     // ...
 * }
 * </pre>
 * In this example:
 * <ul>
 *   <li>Any authenticated user can view posts</li>
 *   <li>Users with either ROLE_MANAGER or ROLE_ADMIN can edit posts</li>
 *   <li>Only users with ROLE_ADMIN can create new posts</li>
 * </ul>
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminModelPermissions {

    /**
     * Permissions required to view entities of this model.
     * User must have at least one of these permissions to view the model's entities.
     * If empty, all users with access to the admin interface can view the model's entities.
     */
    String[] view() default {};

    /**
     * Permissions required to edit entities of this model.
     * User must have at least one of these permissions to edit the model's entities.
     * If empty, all users with access to the admin interface can edit the model's entities.
     */
    String[] edit() default {};

    /**
     * Permissions required to create entities of this model.
     * User must have at least one of these permissions to create new entities of this model.
     * If empty, all users with access to the admin interface can create new entities of this model.
     */
    String[] create() default {};
}
