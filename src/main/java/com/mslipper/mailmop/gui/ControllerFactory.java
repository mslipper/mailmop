package com.mslipper.mailmop.gui;

import com.google.inject.Injector;
import javafx.util.Callback;

public class ControllerFactory {
    private final Callback<Class<?>, Object> callback;

    public ControllerFactory(Injector injector) {
        this.callback = (type) -> {
            Object instance = injector.getInstance(type);

            if (instance == null) {
                try {
                    instance = type.newInstance();
                } catch (Exception exc) {
                    System.err.println("Could not create controller for " + type.getName());
                    throw new RuntimeException(exc);
                }
            }

            return instance;
        };
    }

    public Callback<Class<?>, Object> getCallback() {
        return callback;
    }
}
