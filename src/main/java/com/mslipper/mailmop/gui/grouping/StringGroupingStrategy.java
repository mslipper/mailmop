package com.mslipper.mailmop.gui.grouping;

import com.mslipper.mailmop.domain.MailmopGroup;
import com.mslipper.mailmop.domain.MailmopMessage;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class StringGroupingStrategy implements GroupingStrategy {
    private final Function<MailmopMessage, String> keyExtractor;

    private StringGroupingStrategy(Function<MailmopMessage, String> keyExtractor) {
        this.keyExtractor = keyExtractor;
    }

    @Override
    public List<MailmopGroup> performGrouping(List<MailmopMessage> allMessages) {
        Map<String, List<MailmopMessage>> groups = new HashMap<>();
        allMessages.forEach((message) -> populateGroups(groups, message));

        return groups.entrySet()
            .parallelStream()
            .map((entry) -> new MailmopGroup(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingInt((e) -> ((MailmopGroup) e).getMessages().size()).reversed())
            .collect(Collectors.toList());
    }

    private void populateGroups(Map<String, List<MailmopMessage>> groups, MailmopMessage message) {
        String key = keyExtractor.apply(message);

        List<MailmopMessage> storedList;

        if (groups.containsKey(key)) {
            storedList = groups.get(key);
        } else {
            storedList = new ArrayList<>();
            groups.put(key, storedList);
        }

        storedList.add(message);
    }

    public static class SenderFullGroupingStrategy extends StringGroupingStrategy {
        public SenderFullGroupingStrategy() {
            super(MailmopMessage::getFrom);
        }
    }

    public static class SenderEmailGroupingStrategy extends StringGroupingStrategy {
        static final Function<MailmopMessage, String> keyExtractor = (message) -> {
            String from = message.getFrom();

            if (from.contains("<") && from.contains(">")) {
                return extractNamedEmail(from);
            }

            if (from.contains("@")) {
                return extractSingularEmail(from);
            }

            return from;
        };

        public SenderEmailGroupingStrategy() {
            super(keyExtractor);
        }

        public SenderEmailGroupingStrategy(Function<MailmopMessage, String> override) {
            super(override);
        }

        private static String extractNamedEmail(String from) {
            int start = from.indexOf("<") + 1;
            int end = from.indexOf(">");
            return from.substring(start, end);
        }

        private static String extractSingularEmail(String from) {
            String[] splits = from.split(" ");

            for (String split : splits) {
                if (split.contains("@")) {
                    return split;
                }
            }

            return from;
        }
    }

    public static class SenderDomainGroupingStrategy extends SenderEmailGroupingStrategy {
        public SenderDomainGroupingStrategy() {
            super((message) -> {
                String email = keyExtractor.apply(message);

                if (email.contains("@")) {
                    return extractDomain(email);
                }

                return email;
            });
        }

        private static String extractDomain(String email) {
            String[] splits = email.split("@");

            if (splits.length == 2) {
                return splits[1];
            }

            return email;
        }
    }
}
