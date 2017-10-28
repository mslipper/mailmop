package com.mslipper.mailmop.gui;

import com.mslipper.mailmop.domain.MailmopGroup;
import com.mslipper.mailmop.domain.MailmopMessage;
import com.mslipper.mailmop.gui.grouping.GroupingStrategy;
import com.mslipper.mailmop.gui.grouping.StringGroupingStrategy.SenderEmailGroupingStrategy;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import java.util.*;

public class MessageStore {
    private final ObjectProperty<GroupingStrategy> groupingStrategy =
        new SimpleObjectProperty<>(new SenderEmailGroupingStrategy());

    private final ObservableSet<MailmopMessage> deletedMessages = toObservableSet(new HashSet<>());

    private final ListProperty<MailmopGroup> groups = new SimpleListProperty<>(
        toObservableList(Collections.emptyList()));

    private final ListProperty<MailmopMessage> messages;

    private MessageStore(List<MailmopMessage> messages) {
        this.messages = new SimpleListProperty<>(toObservableList(messages));
        addListeners();
    }

    public synchronized void softDelete(MailmopMessage message) {
        deletedMessages.add(message);
    }

    public synchronized void softDelete(List<MailmopMessage> messages) {
        deletedMessages.addAll(messages);
    }

    public synchronized boolean isDeleted(MailmopMessage message) {
        return deletedMessages.contains(message);
    }

    public ObservableSet<MailmopMessage> deletedMessages() {
        return deletedMessages;
    }

    public ListProperty<MailmopGroup> groupsProperty() {
        return groups;
    }

    public ObjectProperty<GroupingStrategy> groupingStrategyProperty() {
        return groupingStrategy;
    }

    private void addListeners() {
        messages.addListener((observable, oldValue, newValue) -> performGrouping());
        groupingStrategy.addListener((observable, oldValue, newValue) -> performGrouping());
    }

    private void performGrouping() {
        groups.setValue(toObservableList(groupingStrategy.getValue().performGrouping(messages.get())));
    }

    private static <T> ObservableList<T> toObservableList(List<T> list) {
        return FXCollections.observableArrayList(list);
    }

    private static <T> ObservableSet<T> toObservableSet(Set<T> set) {
        return FXCollections.observableSet(set);
    }

    public static MessageStore withMessages(List<MailmopMessage> messages) {
        return new MessageStore(messages);
    }
}
