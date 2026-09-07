package com.drukconnect.drukconnect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
public record AppProperties(

        Auth auth,

        Jwt jwt,

        Otp otp,

        Mail mail,

        Sms sms,

        Vouch vouch

) {


    public record Vouch(
            String invitationUrl,
            Duration invitationTtl,
            String backendUrl,
            Duration responseTokenTtl
    ) {
    }

    public record Auth(

            boolean sendPhoneOtp,

            boolean requirePhoneOtp,

            boolean sendEmailOtp,

            boolean requireEmailOtp,

            String currentGuidelineVersion

    ) {}


    public record Jwt(

            String issuer,

            Duration accessTtl,

            Duration refreshTtl,

            String secretBase64

    ) {}


    public record Otp(

            Duration ttl,

            Duration resendCooldown,

            int maxAttempts,

            int digits,

            String hmacSecretBase64

    ) {}


    public record Mail(

            boolean enabled,

            String from

    ) {}


    public record Sms(

            boolean enabled,

            String endpoint,

            String apiKey,

            String senderId

    ) {}
}