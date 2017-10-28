package com.mslipper.mailmop.backoff;

public interface Retryable<T> {
    T execute() throws Exception;
}
