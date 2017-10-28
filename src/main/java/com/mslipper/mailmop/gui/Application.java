package com.mslipper.mailmop.gui;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mslipper.mailmop.inject.Module;
import javafx.stage.Stage;

public class Application extends javafx.application.Application {
    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) throws Exception {
        Injector injector = Guice.createInjector(new Module(this, stage));
        ControllerFactoryHolder.createFactory(injector);
        injector.getInstance(SceneManager.class).present("home");
    }

    public void stop() {
        System.exit(0);
    }
}