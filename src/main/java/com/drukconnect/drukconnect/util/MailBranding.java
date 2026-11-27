package com.drukconnect.drukconnect.util;

import jakarta.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;

public final class MailBranding {
    private MailBranding() {}
    public static String from(String address) {
        try {
            return new InternetAddress(new InternetAddress(address).getAddress(),
                    "Bhutanese Marketplace", StandardCharsets.UTF_8.name()).toString();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid configured sender address", exception);
        }
    }
}
