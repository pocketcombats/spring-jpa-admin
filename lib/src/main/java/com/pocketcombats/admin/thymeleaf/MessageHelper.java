package com.pocketcombats.admin.thymeleaf;

import org.springframework.context.MessageSourceResolvable;
import org.thymeleaf.spring6.context.IThymeleafRequestContext;

/**
 * Direct access to {@code IThymeleafRequestContext} from template is prohibited
 */
public class MessageHelper {

    public String getMessage(IThymeleafRequestContext context, MessageSourceResolvable message) {
        return context.getMessage(message, false);
    }
}
