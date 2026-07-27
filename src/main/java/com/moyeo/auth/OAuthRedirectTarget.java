package com.moyeo.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * A server-owned identifier for a registered OAuth callback URI.
 * Clients must send this identifier rather than a URI.
 */
public enum OAuthRedirectTarget {
    LOCAL("local"),
    DEV("dev"),
    PROD("prod");

    private final String value;

    OAuthRedirectTarget(String value) {
        this.value = value;
    }

    @JsonCreator
    public static OAuthRedirectTarget from(String value) {
        return Arrays.stream(values())
                .filter(target -> target.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported OAuth redirect target."));
    }

    @JsonValue
    public String value() {
        return value;
    }
}
