package com.pocketcombats.admin.thymeleaf;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;

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
}
