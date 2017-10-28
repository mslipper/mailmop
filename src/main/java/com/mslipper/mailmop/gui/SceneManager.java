package com.mslipper.mailmop.gui;

import com.mslipper.mailmop.Util;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class SceneManager {
    private final Stage stage;

    private Controller controller;

    @Inject
    public SceneManager(Stage stage) {
        this.stage = stage;
        this.stage.setTitle("MailMop");
    }

    public synchronized Scene present(String name) {
        Scene scene = new Scene(loadFXML(name));
        stage.setScene(scene);
        stage.show();
        return scene;
    }

    public Stage getStage() {
        return stage;
    }

    public void bringToFront() {
        stage.toFront();
    }

    public Controller getController() {
        return controller;
    }

    private Parent loadFXML(String name) {
        FXMLLoader loader = new FXMLLoader(Util.resourceUrl("/fxml/" + name + ".fxml"));
        loader.setControllerFactory(ControllerFactoryHolder.getFactory().getCallback());

        try {
            Parent parent = loader.load();
            controller = loader.getController();
            return parent;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
