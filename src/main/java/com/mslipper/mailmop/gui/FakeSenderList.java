package com.mslipper.mailmop.gui;

import com.mslipper.mailmop.Util;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FakeSenderList extends VBox {
    private static final double INSET = 12;

    private static final double ITEM_HEIGHT = 50;

    private static final Color FILL = Color.color(1, 1, 1, 0.30);

    private static final double[] RATIOS = new double[] {
        0.9,
        0.75,
        0.95,
        0.75,
        0.65
    };

    public FakeSenderList() {
        super();

        FXMLLoader loader = new FXMLLoader(Util.resourceUrl("/fxml/fakeSenderList.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        drawChildren();
    }

    private void drawChildren() {
        getChildren().setAll(genChildren());
    }

    @Override
    public void resize(double width, double height) {
        super.resize(width, height);
        drawChildren();
    }

    private List<? extends Node> genChildren() {
        List<Canvas> nodes = new ArrayList<>();

        double width = getWidth();
        double height = getHeight();
        double blockWidth = Math.floor((width - INSET) * 0.95);
        int rows = (int) Math.ceil(height / ITEM_HEIGHT);

        for (int i = 0; i < rows; i++) {
            Canvas canvas = new Canvas(width, ITEM_HEIGHT);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setFill(FILL);
            gc.fillRect(INSET, INSET, blockWidth * RATIOS[i % RATIOS.length], 10);
            gc.fillRect(INSET, INSET + 18, blockWidth, 10);
            nodes.add(canvas);
        }

        return nodes;
    }
}
