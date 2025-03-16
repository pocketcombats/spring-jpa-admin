package com.pocketcombats.admin.core;

import com.pocketcombats.admin.AdminModelPermissions;
import com.pocketcombats.admin.core.action.AdminModelAction;
import com.pocketcombats.admin.core.filter.AdminModelFilter;
import com.pocketcombats.admin.core.links.AdminModelLink;
import com.pocketcombats.admin.core.search.SearchPredicateFactory;
import com.pocketcombats.admin.core.uniqueness.AdminUniqueConstraint;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Represents a registered admin model in the system.
 * <p>
 * This record encapsulates all the configuration and metadata for an admin model,
 * including its entity details, fields, actions, permissions, and more. It is created
 * during application startup by processing classes annotated with {@link com.pocketcombats.admin.AdminModel}.
 */
public record AdminRegisteredModel(
        /**
         * The unique name of the model, used for identification in URLs and API calls.
         */
        String modelName,

        /**
         * The priority of the model, used for ordering in the UI.
         * Higher priority models appear before lower priority models.
         */
        int priority,

        /**
         * The display label for the model, shown in the UI.
         */
        String label,

        /**
         * Details about the entity class associated with this model.
         */
        RegisteredEntityDetails entityDetails,

        /**
         * Whether new entities of this model can be created.
         */
        boolean insertable,

        /**
         * Whether existing entities of this model can be updated.
         */
        boolean updatable,

        /**
         * The number of entities to display per page in the list view.
         */
        int pageSize,

        /**
         * The fields to display in the list view.
         */
        List<AdminModelListField> listFields,

        /**
         * The default order for the list view, or null if no default order is specified.
         */
        @Nullable String defaultOrder,

        /**
         * The factory for creating search predicates, or null if search is not supported.
         */
        @Nullable SearchPredicateFactory searchPredicateFactory,

        /**
         * Filters available for this model list view.
         */
        List<AdminModelFilter> filters,

        /**
         * Fieldsets for the form view, grouping fields into logical sections.
         */
        List<AdminModelFieldset> fieldsets,

        /**
         * Links to other models.
         */
        List<AdminModelLink> links,

        /**
         * Actions that can be performed on entities of this model.
         */
        Map<String, AdminModelAction> actions,

        /**
         * Unique constraints for this model, used for validation on admin site side.
         */
        List<AdminUniqueConstraint> uniqueConstraints,

        /**
         * Permissions required for different actions on this model.
         * This defines who can view, edit, or create entities of this model.
         */
        AdminModelPermissions permissions
) {

}
