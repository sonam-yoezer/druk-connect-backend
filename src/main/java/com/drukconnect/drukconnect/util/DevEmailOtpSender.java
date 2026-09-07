package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.enums.OtpChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DevEmailOtpSender implements OtpSender {
    private static final Logger log = LoggerFactory.getLogger(DevEmailOtpSender.class);
    @Override public OtpChannel channel() { return OtpChannel.EMAIL; }
    @Override public void send(String destination, String otp) {
        log.warn("DEV ONLY - email OTP for {} is {}", destination, otp);
    }
}
