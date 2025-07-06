package org.wowmr.api;

import java.time.Instant;


public class OAuthToken {
    public final String  accessToken;
    public final Instant expiresAt;

    public OAuthToken(String accessToken, Instant expiresAt) {
        this.accessToken = accessToken;
        this.expiresAt   = expiresAt;
    }
}
