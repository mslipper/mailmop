package com.mslipper.mailmop.domain;

import java.util.List;

public class MailmopGroup implements HasMailmopGroup {
    private final String name;

    private final List<MailmopMessage> messages;

    public MailmopGroup(String name, List<MailmopMessage> messages) {
        this.name = name;
        this.messages = messages;
    }

    public String getName() {
        return name;
    }

    public List<MailmopMessage> getMessages() {
        return messages;
    }

    @Override
    public MailmopGroup getGroup() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MailmopGroup that = (MailmopGroup) o;

        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        return getMessages() != null ? getMessages().equals(that.getMessages()) : that.getMessages() == null;
    }

    @Override
    public int hashCode() {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + (getMessages() != null ? getMessages().hashCode() : 0);
        return result;
    }
}
