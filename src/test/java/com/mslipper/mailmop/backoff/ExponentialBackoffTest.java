package com.mslipper.mailmop.backoff;

import com.mslipper.mailmop.exc.MaximumRetriesException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExponentialBackoffTest {
    @Test
    void executeDefault_shouldReturnRetryableValue() throws Exception {
        assertThat(ExponentialBackoff.executeDefault(() -> 1), is(1));
    }

    @Test
    void executeDefault_shouldThrowMaximumRetriesExceptionWhenAllRetriesExhausted() {
        assertThrows(MaximumRetriesException.class, () -> ExponentialBackoff.executeDefault(() -> {
            throw new IOException("Something bad happened.");
        }));
    }

    @Test
    void executeDefault_withBackoffListener_shouldCallOnErrorWhenErrorsHappen() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);

        Integer response = ExponentialBackoff.executeDefault(() -> {
            if (callCount.get() < 3) {
                throw new IOException("oh no!");
            }

            return 1;
        }, (e) -> callCount.incrementAndGet());

        assertThat(response, is(1));
        assertThat(callCount.get(), is(3));
    }
}
