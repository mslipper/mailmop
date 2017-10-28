package com.mslipper.mailmop.gui.grouping;

import com.mslipper.mailmop.domain.HasMailmopGroup;
import com.mslipper.mailmop.domain.MailmopGroup;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public abstract class MailmopGroupCell<T extends HasMailmopGroup> extends ListCell<T> {
    private static final int LEFT_PADDING_BASE = 12;

    private static final int SELECTED_BORDER_WIDTH = 3;

    static final String TEXT_CLASS = "text";

    static final String TEXT_CLASS_SMALL = "text-small";

    private final HBox box = new HBox();

    private final Pane spacer = new Pane();

    private final VBox textVbox = new VBox();

    private final Label senderName = new Label();

    private final Label senderAddress = new Label();

    @Override
    public void updateSelected(boolean selected) {
        super.updateSelected(selected);
        textVbox.setPadding(new Insets(6, 12, 6,
            selected ? LEFT_PADDING_BASE - SELECTED_BORDER_WIDTH : LEFT_PADDING_BASE));
    }

    public abstract Node updateRightSide(T wrapper);

    @Override
    protected void updateItem(T wrapper, boolean empty) {
        super.updateItem(wrapper, empty);

        setText(null);

        if (empty) {
            setGraphic(null);
            return;
        }

        Node rightSide = updateRightSide(wrapper);
        MailmopGroup item = wrapper.getGroup();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox.setHgrow(textVbox, Priority.SOMETIMES);
        HBox.setHgrow(rightSide, Priority.NEVER);
        spacer.setMinWidth(20);

        String name = item.getName();

        if (name.contains("<") && name.contains(">")) {
            int addrIndex = name.indexOf("<");
            senderName.setText(name.substring(0, addrIndex));
            senderAddress.setText(name.substring(addrIndex));
            senderName.getStyleClass().add(TEXT_CLASS);
            senderAddress.getStyleClass().add(TEXT_CLASS);
            senderAddress.getStyleClass().add(TEXT_CLASS_SMALL);
            textVbox.getChildren().setAll(
                senderName,
                senderAddress
            );
        } else {
            senderName.setText(name);
            senderName.getStyleClass().add(TEXT_CLASS);
            textVbox.getChildren().setAll(senderName);
        }

        textVbox.setAlignment(Pos.CENTER_LEFT);
        textVbox.setPadding(new Insets(6, 12, 6, LEFT_PADDING_BASE));
        box.getChildren().setAll(textVbox, spacer, rightSide);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefHeight(54);
        box.setMaxHeight(54);
        box.setMinHeight(54);
        box.setMinWidth(150);
        setGraphic(box);
    }
}
