package com.mslipper.mailmop.gui;

import com.mslipper.mailmop.domain.MailmopMessage;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.util.Callback;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;

public class TimeAgoCellValueFactory<S, T> implements Callback<CellDataFeatures<MailmopMessage, T>, ObservableValue<T>> {
    private static final PrettyTime pt = new PrettyTime();

    @Override
    @SuppressWarnings("unchecked")
    public ObservableValue<T> call(CellDataFeatures<MailmopMessage, T> param) {
        return (ObservableValue<T>) new ReadOnlyStringWrapper(pt.format(new Date(param.getValue().getReceivedAt())));
    }
}
