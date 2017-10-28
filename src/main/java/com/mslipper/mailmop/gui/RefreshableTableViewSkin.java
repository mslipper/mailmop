package com.mslipper.mailmop.gui;

import com.sun.javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class RefreshableTableViewSkin<T> extends TableViewSkin<T> {
    public RefreshableTableViewSkin(TableView<T> tableView) {
        super(tableView);
    }

    public void refresh() {
        super.flow.recreateCells();
    }
}
