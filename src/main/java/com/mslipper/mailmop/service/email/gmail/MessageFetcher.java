package com.mslipper.mailmop.service.email.gmail;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.Gmail.Users.Messages;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.mslipper.mailmop.Util;
import com.mslipper.mailmop.backoff.ExponentialBackoff;
import com.mslipper.mailmop.backoff.Retryable;
import com.mslipper.mailmop.domain.MailmopMessage;
import com.mslipper.mailmop.domain.event.MessagesFetchedEvent;
import com.mslipper.mailmop.domain.event.ProgressEvent;
import com.mslipper.mailmop.domain.event.ProgressListener;
import com.mslipper.mailmop.exc.MaximumRetriesException;
import com.mslipper.mailmop.service.ForkJoinPoolTaskExecutor;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class MessageFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageFetcher.class);

    private final Gmail gmail;

    private final ForkJoinPoolTaskExecutor taskExecutor;

    private final long maxMessages;

    private final Object lock = new Object();

    MessageFetcher(Gmail gmail,
                   ForkJoinPoolTaskExecutor taskExecutor,
                   long maxMessages) {
        this.gmail = gmail;
        this.taskExecutor = taskExecutor;
        this.maxMessages = maxMessages;
    }

    void fetch(ProgressListener<MessagesFetchedEvent> listener) {
        try {
            ProgressEvent.Factory progress = ProgressEvent.Factory.indeterminate();
            ListMessagesResponse response = ExponentialBackoff.executeDefault(new RetryablePageFetch());
            List<Message> messages = new ArrayList<>();

            int pageNumber = 0;

            while (response.getMessages() != null) {
                if (messages.size() >= maxMessages) {
                    break;
                }

                pageNumber++;

                List<Message> resMessages = response.getMessages();
                messages.addAll(resMessages);
                listener.onProgress(progress.create(pageNumber));

                if (response.getNextPageToken() == null) {
                    break;
                }

                String pageToken = response.getNextPageToken();
                response = ExponentialBackoff.executeDefault(new RetryablePageFetch(pageToken));
            }

            resolveMessages(messages, listener);
        } catch (MaximumRetriesException e) {
            listener.onFailure(e);
        }
    }

    private void resolveMessages(List<Message> messageIds,
                                 ProgressListener<MessagesFetchedEvent> listener) {
        List<Message> deduped = Util.deduplicateBy(messageIds, Message::getId);
        ProgressEvent.Factory progress = ProgressEvent.Factory.determinate(deduped.size());
        List<List<Message>> partitions = ListUtils.partition(deduped, 100);
        List<MailmopMessage> resolvedMessages = new ArrayList<>();

        try {
            taskExecutor.execute(() -> partitions.parallelStream().forEach((partition) -> {
                List<MailmopMessage> resMessages;

                try {
                    resMessages = ExponentialBackoff.executeDefault(new RetryableMessageFetch(partition));
                } catch (MaximumRetriesException e) {
                    throw new RuntimeException(e);
                }

                synchronized (lock) {
                    resolvedMessages.addAll(resMessages);
                    listener.onProgress(progress.create(resolvedMessages.size()));
                }
            })).get();
        } catch (Throwable e) {
            listener.onFailure(e);
            return;
        }

        listener.onSuccess(new MessagesFetchedEvent(resolvedMessages));
    }

    private class RetryablePageFetch implements Retryable<ListMessagesResponse> {
        private final String pageToken;

        RetryablePageFetch() {
            this(null);
        }

        RetryablePageFetch(String pageToken) {
            this.pageToken = pageToken;
        }

        @Override
        public ListMessagesResponse execute() throws Exception {
            Messages.List messageList = gmail.users().messages().list(Constants.getUser());

            if (pageToken != null) {
                messageList.setPageToken(pageToken);
            }

            return messageList.execute();
        }
    }

    private class RetryableMessageFetch implements Retryable<List<MailmopMessage>> {
        private final List<Message> inputMessages;

        RetryableMessageFetch(List<Message> inputMessages) {
            this.inputMessages = inputMessages;
        }

        @Override
        public List<MailmopMessage> execute() throws Exception {
            List<MailmopMessage> outputMessages = new ArrayList<>();

            AtomicJsonBatchCallback<Message> callback = new AtomicJsonBatchCallback<Message>() {
                @Override
                public void onSuccess(Message message, HttpHeaders responseHeaders) throws IOException {
                    outputMessages.add(convertGmailMessage(message));
                }
            };

            BatchRequest batch = gmail.batch();

            for (Message message : inputMessages) {
                gmail.users().messages().get(Constants.getUser(), message.getId()).queue(batch, callback);
            }

            batch.execute();

            if (callback.hasErrors()) {
                throw new IOException("Batch did not complete successfully.");
            }

            return outputMessages;
        }

        private MailmopMessage convertGmailMessage(Message message) {
            List<MessagePartHeader> headers = message.getPayload().getHeaders();

            return MailmopMessage.create(
                message.getId(),
                findFrom(headers),
                findSubject(headers),
                message.getInternalDate()
            );
        }

        private String findFrom(List<MessagePartHeader> headers) {
            return findHeader(headers, "from");
        }

        private String findSubject(List<MessagePartHeader> headers) {
            return findHeader(headers, "subject");
        }

        private String findHeader(List<MessagePartHeader> headers, String header) {
            for (MessagePartHeader comparison : headers) {
                if (comparison.getName().trim().equalsIgnoreCase(header)) {
                    return comparison.getValue();
                }
            }

            return null;
        }
    }

}
