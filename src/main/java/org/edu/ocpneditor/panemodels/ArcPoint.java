package org.edu.ocpneditor.panemodels;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;

public class ArcPoint extends Button implements Selectable {

    private double prevLayoutX;
    private double prevLayoutY;

    private Arc parent;

    public ArcPoint(Arc parent, double x, double y) {
        super();
        getStyleClass().add("arcpoint");

        setLayoutX(x);
        setLayoutY(y);

        translateXProperty().bind(widthProperty().divide(-2));
        translateYProperty().bind(widthProperty().divide(-2));

        prevLayoutX = getLayoutX();
        prevLayoutY = getLayoutY();
    }

    public void setPrevLayout(double x, double y) {
        prevLayoutX = x;
        prevLayoutY = y;
    }

    public double getPrevLayoutX() {
        return prevLayoutX;
    }

    public double getPrevLayoutY() {
        return prevLayoutY;
    }

    @Override
    public void setSelectedStyle() {
        setOpacity(1);
    }

    @Override
    public void setUnselectedStyle() {
        setOpacity(0);
    }

    @Override
    public List<Node> asChild() {
        return List.of(this);
    }
}
