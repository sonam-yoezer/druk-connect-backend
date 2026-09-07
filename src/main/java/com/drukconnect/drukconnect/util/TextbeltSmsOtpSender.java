package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.config.AppProperties;
import com.drukconnect.drukconnect.enums.OtpChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(
        prefix = "app.sms",
        name = "enabled",
        havingValue = "true"
)
public class TextbeltSmsOtpSender implements OtpSender {

    private static final Logger log =
            LoggerFactory.getLogger(TextbeltSmsOtpSender.class);

    private final RestClient restClient;
    private final AppProperties properties;

    public TextbeltSmsOtpSender(
            RestClient.Builder restClientBuilder,
            AppProperties properties
    ) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public OtpChannel channel() {
        return OtpChannel.PHONE;
    }

    @Override
    public void send(
            String destination,
            String otp
    ) {

        log.info(
                "PHONE OTP sender invoked. phone={}",
                maskPhone(destination)
        );

        log.info(
                "About to hit Textbelt endpoint: {}",
                properties.sms().endpoint()
        );

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add(
                "phone",
                destination
        );

        form.add(
                "message",
                "Your DrukConnect OTP is " + otp
        );

        form.add(
                "key",
                properties.sms().apiKey()
        );

        try {

            TextbeltResponse response =
                    restClient
                            .post()
                            .uri(properties.sms().endpoint())
                            .contentType(
                                    MediaType.APPLICATION_FORM_URLENCODED
                            )
                            .body(form)
                            .retrieve()
                            .body(TextbeltResponse.class);

            log.info(
                    "Textbelt endpoint HIT successfully. response={}",
                    response
            );

            if (response == null) {

                throw new IllegalStateException(
                        "Textbelt returned empty response"
                );
            }

            if (!Boolean.TRUE.equals(response.success())) {

                throw new IllegalStateException(
                        "Textbelt SMS failed: "
                                + response.error()
                );
            }

            log.info(
                    "SMS successfully sent through Textbelt. phone={}, textId={}, quotaRemaining={}",
                    maskPhone(destination),
                    response.textId(),
                    response.quotaRemaining()
            );

        } catch (Exception ex) {

            log.error(
                    "Textbelt SMS request failed. endpoint={}, phone={}, error={}",
                    properties.sms().endpoint(),
                    maskPhone(destination),
                    ex.getMessage(),
                    ex
            );

            throw ex;
        }
    }

    private String maskPhone(
            String phone
    ) {

        if (
                phone == null
                        ||
                        phone.length() <= 4
        ) {
            return "****";
        }

        return "****"
                + phone.substring(
                phone.length() - 4
        );
    }

    private record TextbeltResponse(
            Boolean success,
            Integer quotaRemaining,
            Long textId,
            String error
    ) {}
}