package com.pocketcombats.admin.core.permission;

import com.pocketcombats.admin.AdminModelPermissions;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;

/**
 * Implementation of the {@link AdminPermissionService} interface.
 * This service checks if a user has the required permissions for a specific action on a model.
 * <p>
 * It uses Spring Security's authentication context to check if the current user has the required permissions.
 */
@Service
public class AdminPermissionServiceImpl implements AdminPermissionService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminPermissionServiceImpl.class);

    @Override
    public boolean canView(AdminRegisteredModel model) {
        AdminModelPermissions permissions = model.permissions();
        boolean hasPermission = hasAnyPermission(permissions.view());
        LOG.trace("View permission for model {}: {}", model.modelName(), hasPermission);
        return hasPermission;
    }

    @Override
    public boolean canEdit(AdminRegisteredModel model) {
        AdminModelPermissions permissions = model.permissions();
        boolean hasPermission = hasAnyPermission(permissions.edit());
        LOG.trace("Edit permission for model {}: {}", model.modelName(), hasPermission);
        return hasPermission;
    }

    @Override
    public boolean canCreate(AdminRegisteredModel model) {
        AdminModelPermissions permissions = model.permissions();
        boolean hasPermission = hasAnyPermission(permissions.create());
        LOG.trace("Create permission for model {}: {}", model.modelName(), hasPermission);
        return hasPermission;
    }

    /**
     * Checks if the current user has any of the specified permissions.
     * If no permissions are specified (empty array), returns true.
     * <p>
     * This method uses Spring Security's authentication context to check if the current user
     * has any of the specified permissions. It first checks if the user is authenticated,
     * then checks if the user has any of the required permissions.
     */
    private static boolean hasAnyPermission(String[] permissions) {
        // If no permissions are required, allow access
        if (permissions == null || permissions.length == 0) {
            LOG.trace("No permissions required, access allowed");
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            LOG.warn("User not authenticated, access denied");
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null) {
            LOG.warn("User has no authorities, access denied");
            return false;
        }

        boolean hasPermission = Arrays.stream(permissions)
                .anyMatch(permission -> authorities.stream()
                        .anyMatch(authority -> authority.getAuthority().equals(permission)));

        if (LOG.isTraceEnabled()) {
            if (hasPermission) {
                LOG.trace("User has at least one required permission: {}", String.join(", ", permissions));
            } else {
                LOG.trace("User does not have any of the required permissions: {}", String.join(", ", permissions));
            }
        }

        return hasPermission;
    }
}
