package com.pocketcombats.admin.core;

import com.pocketcombats.admin.core.field.AdminFormFieldValueAccessor;
import com.pocketcombats.admin.core.field.AutocompleteOptionsAccessor;
import com.pocketcombats.admin.core.permission.AdminPermissionService;
import com.pocketcombats.admin.data.form.AdminSelectOption;
import com.pocketcombats.admin.data.form.AdminSelectOptionsResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class AdminModelOptionsServiceImpl implements AdminModelOptionsService {

    private final AdminModelRegistry registry;
    private final AdminPermissionService permissionService;
    private final int pageSize;

    public AdminModelOptionsServiceImpl(
            AdminModelRegistry registry,
            AdminPermissionService permissionService,
            int pageSize
    ) {
        this.registry = registry;
        this.permissionService = permissionService;
        this.pageSize = pageSize;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public AdminSelectOptionsResponse options(
            String modelName,
            String fieldName,
            @Nullable String query,
            int page
    ) throws UnknownModelException {
        return resolveField(modelName, fieldName).collectOptions(query, page, pageSize);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public AdminSelectOptionsResponse resolve(
            String modelName,
            String fieldName,
            String value
    ) throws UnknownModelException {
        AdminSelectOption option = resolveField(modelName, fieldName).resolveOption(value);
        return new AdminSelectOptionsResponse(option == null ? List.of() : List.of(option), false);
    }

    private AutocompleteOptionsAccessor resolveField(String modelName, String fieldName) throws UnknownModelException {
        AdminRegisteredModel model = registry.resolve(modelName);
        if (!permissionService.canView(model)) {
            throw new AccessDeniedException("You don't have permission to view " + modelName);
        }
        AdminModelField field = model.findFormField(fieldName)
                .orElseThrow(() -> new UnknownModelException(
                        "model '" + modelName + "' has no field '" + fieldName + "'"));
        AdminFormFieldValueAccessor accessor = field.valueAccessor();
        if (!(accessor instanceof AutocompleteOptionsAccessor optionsAccessor)
                || !optionsAccessor.autocompleteSupported()) {
            throw fieldDoesNotServeOptions(modelName, fieldName);
        }
        return optionsAccessor;
    }

    private static UnknownModelException fieldDoesNotServeOptions(String modelName, String fieldName) {
        return new UnknownModelException(
                "field '" + fieldName + "' of model '" + modelName + "' does not serve options");
    }
}
