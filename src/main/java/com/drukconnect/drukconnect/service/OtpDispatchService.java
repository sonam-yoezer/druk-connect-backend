package com.drukconnect.drukconnect.service;

import com.drukconnect.drukconnect.enums.OtpChannel;
import com.drukconnect.drukconnect.util.OtpSender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class OtpDispatchService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OtpDispatchService.class
            );

    private final Map<OtpChannel, OtpSender> senders =
            new EnumMap<>(OtpChannel.class);

    public OtpDispatchService(
            List<OtpSender> senderList
    ) {

        for (OtpSender sender : senderList) {

            log.info(
                    "Registering OTP sender. channel={}, implementation={}",
                    sender.channel(),
                    sender.getClass().getSimpleName()
            );

            senders.put(
                    sender.channel(),
                    sender
            );
        }
    }

    public void send(
            OtpChannel channel,
            String destination,
            String otp
    ) {

        log.info(
                "OTP dispatch requested. channel={}",
                channel
        );

        OtpSender sender =
                senders.get(channel);

        if (sender == null) {

            log.error(
                    "No OTP sender registered for channel={}",
                    channel
            );

            throw new IllegalStateException(
                    "No OTP sender configured for "
                            + channel
            );
        }

        log.info(
                "Using OTP sender {} for channel={}",
                sender.getClass()
                        .getSimpleName(),
                channel
        );

        sender.send(
                destination,
                otp
        );
    }
}