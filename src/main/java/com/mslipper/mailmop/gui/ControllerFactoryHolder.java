package com.mslipper.mailmop.gui;

import com.google.inject.Injector;

public class ControllerFactoryHolder {
    private static ControllerFactory INSTANCE;

    public static void createFactory(Injector injector) {
        INSTANCE = new ControllerFactory(injector);
    }

    public static ControllerFactory getFactory() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Factory has not been created yet.");
        }

        return INSTANCE;
    }
}
