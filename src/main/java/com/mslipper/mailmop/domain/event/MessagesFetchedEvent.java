package com.mslipper.mailmop.domain.event;

import com.mslipper.mailmop.domain.MailmopMessage;

import java.util.List;

public class MessagesFetchedEvent implements Event {
    private final List<MailmopMessage> messages;

    public MessagesFetchedEvent(List<MailmopMessage> messages) {
        this.messages = messages;
    }

    public List<MailmopMessage> getMessages() {
        return messages;
    }
}
