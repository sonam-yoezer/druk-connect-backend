package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.config.AppProperties;
import com.drukconnect.drukconnect.enums.OtpChannel;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "app.mail",
        name = "enabled",
        havingValue = "true"
)
public class SmtpEmailOtpSender
        implements OtpSender {


    private final JavaMailSender mailSender;

    private final AppProperties properties;


    public SmtpEmailOtpSender(
            JavaMailSender mailSender,
            AppProperties properties
    ) {

        this.mailSender =
                mailSender;

        this.properties =
                properties;
    }


    /*
     * =========================================================
     * CHANNEL
     * =========================================================
     */
    @Override
    public OtpChannel channel() {

        return OtpChannel.EMAIL;
    }


    /*
     * =========================================================
     * SEND EMAIL OTP
     * =========================================================
     */
    @Override
    public void send(
            String email,
            String otp
    ) {

        log.info(
                "Sending email OTP to [{}]",
                email
        );


        SimpleMailMessage message =
                new SimpleMailMessage();


        message.setFrom(
                properties.mail().from()
        );


        message.setTo(
                email
        );


        message.setSubject(
                "DrukConnect Email Verification"
        );


        message.setText(
                """
                Welcome to DrukConnect.

                Your email verification OTP is:

                %s

                This OTP will expire in 5 minutes.

                If you did not create a DrukConnect account,
                please ignore this email.

                DrukConnect
                """.formatted(otp)
        );


        mailSender.send(
                message
        );


        log.info(
                "Email OTP successfully submitted to SMTP for [{}]",
                email
        );
    }
}