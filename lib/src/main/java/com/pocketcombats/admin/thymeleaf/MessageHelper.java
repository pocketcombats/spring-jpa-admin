package com.pocketcombats.admin.thymeleaf;

import com.pocketcombats.admin.core.UniquenessViolationError;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.ObjectError;

/**
 * Resolves {@link MessageSourceResolvable}s (e.g. validation errors) from templates.
 */
public class MessageHelper {

    private final MessageSource messageSource;

    public MessageHelper(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(MessageSourceResolvable message) {
        return messageSource.getMessage(message, LocaleContextHolder.getLocale());
    }

    /**
     * The error as a {@link UniquenessViolationError}, or {@code null} for any other error kind.
     * Lets templates render conflict-specific extras (the "view conflicting entity" link) without
     * type checks, which Thymeleaf's expression language restricts.
     */
    public @Nullable UniquenessViolationError uniquenessViolation(ObjectError error) {
        return error instanceof UniquenessViolationError violation ? violation : null;
    }
}
