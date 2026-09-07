package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.enums.OtpChannel;

public interface OtpSender {

    OtpChannel channel();

    void send(
            String destination,
            String otp
    );
}