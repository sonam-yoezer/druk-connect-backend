package com.drukconnect.drukconnect.util;

public interface SmsOtpSender {

    void sendOtp(
            String phoneNumber,
            String otp
    );
}