package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.enums.authentication.OtpChannel;

public interface OtpSender {

    OtpChannel channel();

    void send(
            String destination,
            String otp
    );
}