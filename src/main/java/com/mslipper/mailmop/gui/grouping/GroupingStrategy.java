package com.mslipper.mailmop.gui.grouping;

import com.mslipper.mailmop.domain.MailmopGroup;
import com.mslipper.mailmop.domain.MailmopMessage;

import java.util.List;

public interface GroupingStrategy {
    List<MailmopGroup> performGrouping(List<MailmopMessage> allMessages);
}
