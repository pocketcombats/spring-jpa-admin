package com.pocketcombats.admin.plugin.auth.service;

import jakarta.servlet.http.HttpServletRequest;

public abstract class RequestAddress {

    private RequestAddress() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor != null ? forwardedFor : request.getRemoteAddr();
    }
}
