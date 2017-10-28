package com.mslipper.mailmop.service.email.gmail.event;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.mslipper.mailmop.domain.event.Event;

public class AuthorizationEvent implements Event {
    private final Credential credential;

    private final TokenResponse tokenResponse;

    public AuthorizationEvent(Credential credential, TokenResponse tokenResponse) {
        this.credential = credential;
        this.tokenResponse = tokenResponse;
    }

    public Credential getCredential() {
        return credential;
    }

    public TokenResponse getTokenResponse() {
        return tokenResponse;
    }
}
