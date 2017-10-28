package com.mslipper.mailmop.service.email.gmail;

import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

class AtomicJsonBatchCallback<T> extends JsonBatchCallback<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtomicJsonBatchCallback.class);

    private final AtomicBoolean hasErrors = new AtomicBoolean(false);

    @Override
    public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
        LOGGER.error("Received batch error code {}: {}", e.getCode(), e.toPrettyString());
        hasErrors.set(true);
    }

    @Override
    public void onSuccess(T t, HttpHeaders responseHeaders) throws IOException {}

    boolean hasErrors() {
        return hasErrors.get();
    }
}
