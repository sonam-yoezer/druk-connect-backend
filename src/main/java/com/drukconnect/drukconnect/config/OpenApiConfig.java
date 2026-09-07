package com.drukconnect.drukconnect.config;

import com.drukconnect.drukconnect.controller.AuthController;
import com.drukconnect.drukconnect.controller.VouchController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /*
     * ============================================================
     * GENERAL DRUKCONNECT OPENAPI CONFIGURATION
     * ============================================================
     */
    @Bean
    public OpenAPI drukConnectOpenAPI() {

        return new OpenAPI()

                .info(
                        new Info()
                                .title("DrukConnect API")
                                .version("1.0.0")
                                .description("""
                                        DrukConnect REST API documentation.

                                        Authentication:
                                        - Access Token: 15 minutes
                                        - Refresh Token: 7 days
                                        - Bearer JWT authentication

                                        Current Roles:
                                        - ADMIN
                                        - ENDUSER
                                        """)
                                .contact(
                                        new Contact()
                                                .name("DrukConnect")
                                )
                )

                /*
                 * JWT AUTHORIZATION CONFIGURATION
                 *
                 * This creates the "Authorize" button in Swagger.
                 */
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "bearerAuth",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description(
                                                        "Enter your access token without the word Bearer"
                                                )
                                )
                );
    }


    /*
     * ============================================================
     * AUTH CONTROLLER DOCUMENTATION
     * ============================================================
     *
     * Swagger URL:
     * /v3/api-docs/auth
     *
     * Only APIs from AuthController will appear.
     */
    @Bean
    public GroupedOpenApi authenticationApi() {

        return GroupedOpenApi.builder()

                .group("auth")

                .displayName("Authentication APIs")

                /*
                 * Match APIs from the AuthController
                 */
                .pathsToMatch(
                        "/api/v1/auth/**"
                )

                /*
                 * Extra protection:
                 * only methods belonging to AuthController
                 */
                .addOpenApiMethodFilter(
                        method ->
                                AuthController.class
                                        .isAssignableFrom(
                                                method.getDeclaringClass()
                                        )
                )

                .build();
    }


    /*
     * ============================================================
     * VOUCH CONTROLLER DOCUMENTATION
     * ============================================================
     *
     * Swagger URL:
     * /v3/api-docs/vouch
     *
     * Only APIs from VouchController will appear.
     */
    @Bean
    public GroupedOpenApi vouchApi() {

        return GroupedOpenApi.builder()

                .group("vouch")

                .displayName("Vouch APIs")

                .pathsToMatch(
                        "/api/v1/vouch-requests/**"
                )

                .addOpenApiMethodFilter(
                        method ->
                                VouchController.class
                                        .isAssignableFrom(
                                                method.getDeclaringClass()
                                        )
                )

                .build();
    }
}
