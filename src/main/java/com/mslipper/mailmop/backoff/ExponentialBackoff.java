package com.mslipper.mailmop.backoff;

import com.mslipper.mailmop.exc.MaximumRetriesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class ExponentialBackoff<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExponentialBackoff.class);

    private final int maxRetries;

    private final int minWait;

    private final int maxWait;

    private final Retryable<T> retryable;

    private BackoffListener listener;

    private int retryCount = 0;

    private Random random = new Random();

    private ExponentialBackoff(int maxRetries, int minWait, int maxWait, Retryable<T> retryable, BackoffListener listener) {
        this.maxRetries = maxRetries;
        this.minWait = minWait;
        this.maxWait = maxWait;
        this.retryable = retryable;
        this.listener = listener;
    }

    private T execute() throws MaximumRetriesException {
        try {
            LOGGER.debug("Retry {} of {}: Attempting.", retryCount, maxRetries);
            return retryable.execute();
        } catch (Exception e) {
            LOGGER.warn("Retry {} of {}: Received error.", retryCount, maxRetries, e);

            if (retryCount == maxRetries) {
                throw new MaximumRetriesException("Reached maximum retries.", e);
            }

            if (listener != null) {
                listener.onError(e);
            }

            retryCount++;
            doWait();
            return execute();
        }
    }

    private void doWait() {
        try {
            long wait = Math.min(minWait + (int) Math.pow(2, retryCount - 1) * 150 + jitter(), maxWait);
            LOGGER.info("Retry {} of {}: Waiting for {} ms.", retryCount, maxRetries, wait);
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private int jitter() {
        return random.nextInt(500);
    }

    public static <T> T executeDefault(Retryable<T> retryable) throws MaximumRetriesException {
        return executeDefault(retryable, null);
    }

    public static <T> T executeDefault(Retryable<T> retryable, BackoffListener listener) throws MaximumRetriesException {
        ExponentialBackoff<T> exponentialBackoff =
            new ExponentialBackoff<>(10, 0, 30000, retryable, listener);
        return exponentialBackoff.execute();
    }

    @FunctionalInterface
    public interface BackoffListener {
        void onError(Exception e);
    }
}
