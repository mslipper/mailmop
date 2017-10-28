package com.mslipper.mailmop.service.email;

import com.mslipper.mailmop.domain.MailmopMessage;
import com.mslipper.mailmop.domain.event.Listener;
import com.mslipper.mailmop.domain.event.MessagesDeletedEvent;
import com.mslipper.mailmop.domain.event.MessagesFetchedEvent;
import com.mslipper.mailmop.domain.event.ProgressListener;
import com.mslipper.mailmop.service.email.gmail.event.AuthorizationEvent;

import java.io.IOException;
import java.util.List;

public interface EmailService {
    void authorize(AuthorizationListener listener);

    void fetchMessages(ProgressListener<MessagesFetchedEvent> listener);

    void deleteMessages(List<MailmopMessage> messages, ProgressListener<MessagesDeletedEvent> listener);

    interface AuthorizationListener extends Listener<AuthorizationEvent> {
        void onOAuthUrl(String url);
    }
}
