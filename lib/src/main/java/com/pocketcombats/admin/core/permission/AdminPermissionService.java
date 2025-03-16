package com.pocketcombats.admin.core.permission;

import com.pocketcombats.admin.AdminModelPermissions;
import com.pocketcombats.admin.core.AdminRegisteredModel;

/**
 * Service for checking if a user has the required permissions for a specific action on a model.
 * <p>
 * This service is used throughout the admin interface to control access to models and their actions.
 * It checks the current user's permissions against the permissions defined in the {@link AdminModelPermissions}
 * annotation on the model.
 * <p>
 * The service provides methods to check if the current user has permission to view, edit, or create
 * entities of a specific model. If no permissions are defined for an action, it is assumed that
 * all users with access to the admin interface can perform that action.
 */
public interface AdminPermissionService {

    /**
     * Checks if the current user has permission to view entities of the specified model.
     * <p>
     * This method checks if the current user has any of the permissions specified in the
     * {@link AdminModelPermissions#view()} array of the model. If the array is empty,
     * all users with access to the admin interface can view the model's entities.
     */
    boolean canView(AdminRegisteredModel model);

    /**
     * Checks if the current user has permission to edit entities of the specified model.
     * <p>
     * This method checks if the current user has any of the permissions specified in the
     * {@link AdminModelPermissions#edit()} array of the model. If the array is empty,
     * all users with access to the admin interface can edit the model's entities.
     */
    boolean canEdit(AdminRegisteredModel model);

    /**
     * Checks if the current user has permission to create entities of the specified model.
     * <p>
     * This method checks if the current user has any of the permissions specified in the
     * {@link AdminModelPermissions#create()} array of the model. If the array is empty,
     * all users with access to the admin interface can create new entities of this model.
     */
    boolean canCreate(AdminRegisteredModel model);
}
