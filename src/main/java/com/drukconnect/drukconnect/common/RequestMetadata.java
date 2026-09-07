package com.drukconnect.drukconnect.common;

import jakarta.servlet.http.HttpServletRequest;

public record RequestMetadata(String ipAddress, String userAgent) {
    public static RequestMetadata from(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        if (ua != null && ua.length() > 500) ua = ua.substring(0, 500);
        return new RequestMetadata(ip, ua);
    }
}
