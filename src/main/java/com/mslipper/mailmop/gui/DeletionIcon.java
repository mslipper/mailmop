package com.mslipper.mailmop.gui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeletionIcon extends Pane {
    private static final Color DEFAULT_COLOR = Color.rgb(89, 73, 255);

    private static final Color DONE_COLOR = Color.rgb(15, 206, 67);

    private final AtomicBoolean isAnimating = new AtomicBoolean(false);

    private final Deque<Double> progressPoints = new ArrayDeque<>();

    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);

    private final Arc arc = new Arc();

    private final Circle circle = new Circle();

    private final Image trashIcon = new Image("/images/trash.png");

    private final Image checkIcon = new Image("/images/check.png");

    private final ImageView iconView = new ImageView();

    public DeletionIcon() {
        super();

        setPrefSize(31, 31);
        setMaxSize(31, 31);
        drawChildren();

        progress.addListener((obs, oldVal, newVal) -> {
            double doubleVal = (double) newVal;
            progressPoints.add(doubleVal * -360);

            if (!isAnimating.get()) {
                animate();
            }

            if (doubleVal >= 1.0) {
                arc.setStroke(DONE_COLOR);
                circle.setFill(DONE_COLOR);
                iconView.setImage(checkIcon);
                iconView.setY(12);
            }
        });
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    private void drawChildren() {
        getChildren().setAll(genChildren());
    }

    private List<? extends Node> genChildren() {
        List<Node> nodes = new ArrayList<>();

        arc.setCenterX(16);
        arc.setCenterY(16);
        arc.setRadiusX(15);
        arc.setRadiusY(15);
        arc.setStartAngle(90);
        arc.setStroke(DEFAULT_COLOR);
        arc.setStrokeWidth(2);
        arc.setType(ArcType.OPEN);
        nodes.add(arc);

        circle.setCenterX(16);
        circle.setCenterY(16);
        circle.setRadius(10);
        circle.setFill(DEFAULT_COLOR);
        nodes.add(circle);


        iconView.setFitWidth(10);
        iconView.setPreserveRatio(true);
        iconView.setCache(true);
        iconView.setSmooth(false);
        iconView.setX(11);
        iconView.setY(11);
        iconView.setImage(trashIcon);
        nodes.add(iconView);

        return nodes;
    }

    private void animate() {
        if (progressPoints.isEmpty()) {
            isAnimating.set(false);
            return;
        }

        isAnimating.set(true);
        EventHandler<ActionEvent> onFinished = (e) -> animate();
        Timeline timeline = new Timeline();
        timeline.setCycleCount(1);
        KeyValue kv = new KeyValue(arc.lengthProperty(), progressPoints.poll(), Interpolator.EASE_BOTH);
        KeyFrame kf = new KeyFrame(Duration.millis(150), "arcAnim", onFinished, kv);
        timeline.getKeyFrames().add(kf);
        timeline.play();
    }
}
