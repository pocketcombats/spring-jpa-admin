package com.pocketcombats.admin.core;

import org.jspecify.annotations.Nullable;
import org.springframework.validation.ObjectError;

import java.util.Objects;

/**
 * Global form error reporting a uniqueness-constraint conflict with an existing entity. Carries the
 * conflicting entity's admin model name and id so the form can render a link to it alongside the
 * plain-text message.
 */
public class UniquenessViolationError extends ObjectError {

    private final String conflictingModel;
    private final String conflictingEntityId;

    public UniquenessViolationError(
            String objectName,
            String[] codes,
            Object[] arguments,
            String conflictingModel,
            String conflictingEntityId
    ) {
        super(objectName, codes, arguments, null);
        this.conflictingModel = conflictingModel;
        this.conflictingEntityId = conflictingEntityId;
    }

    public String getConflictingModel() {
        return conflictingModel;
    }

    public String getConflictingEntityId() {
        return conflictingEntityId;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        // super.equals compares exact classes, keeping this symmetric with plain ObjectErrors
        if (!(other instanceof UniquenessViolationError that) || !super.equals(other)) {
            return false;
        }
        return conflictingModel.equals(that.conflictingModel)
                && conflictingEntityId.equals(that.conflictingEntityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), conflictingModel, conflictingEntityId);
    }
}
