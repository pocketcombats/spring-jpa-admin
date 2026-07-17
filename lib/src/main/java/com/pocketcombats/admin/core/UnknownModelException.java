package com.pocketcombats.admin.core;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a request references an admin model, entity, or model field that is
 * not registered or no longer exists.
 * Maps to HTTP 404.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UnknownModelException extends Exception {

    public UnknownModelException() {
    }

    public UnknownModelException(String message) {
        super(message);
    }
}
