package com.mslipper.mailmop.domain.event;

import com.mslipper.mailmop.domain.MailmopMessage;

import java.util.List;

public class MessagesDeletedEvent implements Event {
    private final List<MailmopMessage> deletedMessages;

    public MessagesDeletedEvent(List<MailmopMessage> deletedMessages) {
        this.deletedMessages = deletedMessages;
    }

    public List<MailmopMessage> getDeletedMessages() {
        return deletedMessages;
    }
}
