package com.mslipper.mailmop.gui;

import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

public class CheckboxCellFactory<S> extends PaddedCellFactory<S, Boolean> {
    public CheckboxCellFactory(boolean isFirst, boolean isLast) {
        super(isFirst, isLast);
    }

    @Override
    public TableCell<S, Boolean> call(TableColumn<S, Boolean> param) {
        return new TableCell<S, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                if (empty) {
                    setGraphic(null);
                    return;
                }

                applyPadding(this);
                CheckBox checkBox = new CheckBox();
                checkBox.setSelected(item);
                checkBox.setDisable(true);
                setGraphic(checkBox);
            }
        };
    }
}
