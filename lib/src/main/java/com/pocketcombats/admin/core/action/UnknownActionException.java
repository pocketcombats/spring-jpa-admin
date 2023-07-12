package com.pocketcombats.admin.core.action;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UnknownActionException extends Exception {

    public UnknownActionException(String action) {
        super("Unsupported action: " + action);
    }
}
