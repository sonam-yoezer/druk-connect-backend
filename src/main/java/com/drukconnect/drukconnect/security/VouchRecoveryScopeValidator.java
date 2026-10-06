package com.drukconnect.drukconnect.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public class VouchRecoveryScopeValidator
        implements OAuth2TokenValidator<Jwt> {

    private static final String REQUIRED_SCOPE =
            "VOUCH_RECOVERY";


    @Override
    public OAuth2TokenValidatorResult validate(
            Jwt jwt
    ) {

        List<String> scopes =
                jwt.getClaimAsStringList(
                        "scope"
                );


        if (
                scopes != null
                        &&
                        scopes.contains(
                                REQUIRED_SCOPE
                        )
        ) {

            return OAuth2TokenValidatorResult
                    .success();
        }


        return OAuth2TokenValidatorResult
                .failure(
                        new OAuth2Error(
                                "invalid_token",
                                "This token cannot be used for vouch recovery",
                                null
                        )
                );
    }
}