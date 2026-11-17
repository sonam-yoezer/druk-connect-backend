package com.drukconnect.drukconnect.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
public class ResendMailConfiguration {
    @Bean
    ResendMailSender resendMailSender(@Value("${app.mail.resend.api-key:}") String apiKey) {
        if (apiKey.isBlank()) throw new IllegalStateException("RESEND_API_KEY is required when MAIL_PROVIDER=resend");
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(15));
        return new ResendMailSender(RestClient.builder()
                .baseUrl("https://api.resend.com")
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build());
    }
}
