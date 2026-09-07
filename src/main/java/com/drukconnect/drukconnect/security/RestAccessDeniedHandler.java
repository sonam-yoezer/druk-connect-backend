package com.drukconnect.drukconnect.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RestAccessDeniedHandler
        implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public RestAccessDeniedHandler(
            JsonMapper jsonMapper
    ) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put(
                "timestamp",
                Instant.now()
        );

        body.put(
                "status",
                403
        );

        body.put(
                "error",
                "Forbidden"
        );

        body.put(
                "code",
                "ACCESS_DENIED"
        );

        body.put(
                "message",
                "You do not have permission to access this resource"
        );

        body.put(
                "path",
                request.getRequestURI()
        );

        jsonMapper.writeValue(
                response.getOutputStream(),
                body
        );
    }
}