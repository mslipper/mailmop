package com.mslipper.mailmop.domain;

import java.util.Objects;

public class MailmopMessage {
    private final String id;

    private final String from;

    private final String subject;

    private final long receivedAt;

    private MailmopMessage(String id, String from, String subject, long receivedAt) {
        this.id = Objects.requireNonNull(id);
        this.from = Objects.requireNonNull(from);
        this.subject = Objects.requireNonNull(subject);
        this.receivedAt = receivedAt;
    }

    public String getId() {
        return id;
    }

    public String getFrom() {
        return from;
    }

    public String getSubject() {
        return subject;
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MailmopMessage message = (MailmopMessage) o;

        if (getReceivedAt() != message.getReceivedAt()) return false;
        if (!getId().equals(message.getId())) return false;
        if (!getFrom().equals(message.getFrom())) return false;
        return getSubject().equals(message.getSubject());
    }

    @Override
    public int hashCode() {
        int result = getId().hashCode();
        result = 31 * result + getFrom().hashCode();
        result = 31 * result + getSubject().hashCode();
        result = 31 * result + (int) (getReceivedAt() ^ (getReceivedAt() >>> 32));
        return result;
    }

    public static MailmopMessage create(String id, String from, String subject, long receivedAt) {
        if (subject == null) {
            subject = "";
        }

        return new MailmopMessage(id, from, subject, receivedAt);
    }
}
