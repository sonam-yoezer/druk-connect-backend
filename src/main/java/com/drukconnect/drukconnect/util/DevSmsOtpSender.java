package com.drukconnect.drukconnect.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.sms",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class DevSmsOtpSender
        implements SmsOtpSender {


    private static final Logger log =
            LoggerFactory.getLogger(
                    DevSmsOtpSender.class
            );


    @Override
    public void sendOtp(
            String phoneNumber,
            String otp
    ) {

        log.warn(
                "DEV ONLY - phone OTP for {} is {}",
                phoneNumber,
                otp
        );
    }
}