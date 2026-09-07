package com.drukconnect.drukconnect.util;

public interface EmailOtpSender {

    void sendOtp(
            String email,
            String otp
    );
}