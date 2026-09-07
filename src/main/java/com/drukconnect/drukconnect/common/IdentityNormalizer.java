package com.drukconnect.drukconnect.common;

import org.springframework.http.HttpStatus;

import java.util.Locale;

public final class IdentityNormalizer {
    private IdentityNormalizer() {}

    public static String email(String raw) {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }

    public static String phone(String raw) {
        if (raw == null) return null;
        String compact = raw.trim().replaceAll("[\\s()-]", "");

        // Convenience for local Bhutan numbers. Other countries should use E.164, e.g. +614...
        if (compact.matches("\\d{8}")) compact = "+975" + compact;
        else if (compact.matches("975\\d{8}")) compact = "+" + compact;

        if (!compact.matches("^\\+[1-9]\\d{7,14}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PHONE",
                    "Phone number must be in E.164 format, for example +97517123456");
        }
        return compact;
    }

    public static String maskIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        if (identifier.contains("@")) {
            int at = identifier.indexOf('@');
            String local = identifier.substring(0, at);
            String domain = identifier.substring(at);
            return (local.length() <= 2 ? "**" : local.substring(0, 2) + "***") + domain;
        }
        if (identifier.length() <= 4) return "****";
        return "***" + identifier.substring(identifier.length() - 4);
    }
}
