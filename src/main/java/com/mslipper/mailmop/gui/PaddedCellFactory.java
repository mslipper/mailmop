package com.mslipper.mailmop.gui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class PaddedCellFactory<S, T> implements Callback<TableColumn<S, T>, TableCell<S, T>> {
    private final boolean isFirst;

    private final boolean isLast;

    public PaddedCellFactory(boolean isFirst, boolean isLast) {
        this.isFirst = isFirst;
        this.isLast = isLast;
    }

    public PaddedCellFactory() {
        this(false, false);
    }

    protected void applyPadding(TableCell cell) {
        cell.setPadding(new Insets(6, isLast ? 12 : 6, 6, isFirst ? 12 : 6));
    }

    @Override
    public TableCell<S, T> call(TableColumn<S, T> param) {
        return new TableCell<S, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                if (item == getItem()) {
                    return;
                }

                super.updateItem(item, empty);

                if (item == null) {
                    super.setText(null);
                    super.setGraphic(null);
                } else if (item instanceof Node) {
                    super.setText(null);
                    super.setGraphic((Node) item);
                } else {
                    super.setText(item.toString());
                    super.setGraphic(null);
                }

                applyPadding(this);
            }
        };
    }
}
