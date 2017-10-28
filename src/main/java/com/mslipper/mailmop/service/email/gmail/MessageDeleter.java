package com.mslipper.mailmop.service.email.gmail;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.mslipper.mailmop.backoff.ExponentialBackoff;
import com.mslipper.mailmop.backoff.Retryable;
import com.mslipper.mailmop.domain.MailmopMessage;
import com.mslipper.mailmop.domain.event.MessagesDeletedEvent;
import com.mslipper.mailmop.domain.event.ProgressEvent;
import com.mslipper.mailmop.domain.event.ProgressEvent.Factory;
import com.mslipper.mailmop.domain.event.ProgressListener;
import com.mslipper.mailmop.exc.MaximumRetriesException;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class MessageDeleter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageDeleter.class);

    private static final int PARTITION_SIZE = 20;

    private final Gmail gmail;

    MessageDeleter(Gmail gmail) {
        this.gmail = gmail;
    }

    void delete(List<MailmopMessage> messages, ProgressListener<MessagesDeletedEvent> listener) {
        ProgressEvent.Factory factory = Factory.determinate(messages.size());
        List<List<MailmopMessage>> partitions = ListUtils.partition(messages, PARTITION_SIZE);

        int processed = 0;

        try {
            for (List<MailmopMessage> partition : partitions) {
                ExponentialBackoff.executeDefault(new RetryableDeletion(partition));
                processed += partition.size();
                listener.onProgress(factory.create(processed));
            }
        } catch (MaximumRetriesException e) {
            listener.onFailure(e);
            return;
        }

        listener.onSuccess(new MessagesDeletedEvent(messages));
    }

    private class RetryableDeletion implements Retryable<Void> {
        private final List<MailmopMessage> input;

        private RetryableDeletion(List<MailmopMessage> input) {
            this.input = input;
        }

        @Override
        public Void execute() throws Exception {
            BatchRequest batch = gmail.batch();

            AtomicJsonBatchCallback<Message> callback = new AtomicJsonBatchCallback<>();

            for (MailmopMessage message : input) {
                gmail.users().messages().trash(Constants.getUser(), message.getId())
                    .queue(batch, callback);
            }

            batch.execute();

            if (callback.hasErrors()) {
                throw new IOException("Batch did not complete successfully.");
            }

            return null;
        }
    }
}
