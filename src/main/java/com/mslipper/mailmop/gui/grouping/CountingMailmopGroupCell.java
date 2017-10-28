package com.mslipper.mailmop.gui.grouping;

import com.mslipper.mailmop.domain.HasMailmopGroup;
import com.mslipper.mailmop.domain.MailmopGroup;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;

public class CountingMailmopGroupCell<T extends HasMailmopGroup> extends MailmopGroupCell<T> {
    @Override
    public Node updateRightSide(T wrapper) {
        MailmopGroup item = wrapper.getGroup();
        Text countText = new Text(String.valueOf(item.getMessages().size()));
        countText.getStyleClass().add(TEXT_CLASS);
        TextFlow countFlow = new TextFlow(countText);
        countFlow.setMaxHeight(14);
        countFlow.setTextAlignment(TextAlignment.RIGHT);
        return countFlow;
    }

    public static <T extends HasMailmopGroup> Callback<ListView<T>, ListCell<T>> factory() {
        return (param) -> new CountingMailmopGroupCell<>();
    }
}
