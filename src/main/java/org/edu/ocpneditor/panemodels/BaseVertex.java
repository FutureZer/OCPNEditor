package org.edu.ocpneditor.panemodels;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.List;
import java.util.stream.Stream;

public abstract class BaseVertex extends Button implements Selectable {

    private final ObservableList<Arc> outArcs = FXCollections.observableArrayList();
    private final ObservableList<Arc> inArcs = FXCollections.observableArrayList();

    private double prevLayoutX;
    private double prevLayoutY;

    private final double COPY_PADDING = 15;

    private final SimpleStringProperty name = new SimpleStringProperty("");

    private final Label label = new Label();

    protected static int count = 0;
    protected String graphId;

    public BaseVertex(double x, double y) {
        translateXProperty().bind(widthProperty().divide(-2));
        translateYProperty().bind(heightProperty().divide(-2));

        setLayoutX(x);
        setLayoutY(y);

        prevLayoutX = getLayoutX();
        prevLayoutY = getLayoutY();

        initLabel();
    }

    public BaseVertex(BaseVertex copy) {
        translateXProperty().bind(widthProperty().divide(-2));
        translateYProperty().bind(heightProperty().divide(-2));

        setLayoutX(copy.getLayoutX() + COPY_PADDING);
        setLayoutY(copy.getLayoutY() + COPY_PADDING);

        prevLayoutX = getLayoutX();
        prevLayoutY = getLayoutY();

        initLabel();

        name.set(copy.getName());
    }

    private void initLabel() {
        label.translateXProperty().bind(label.widthProperty().divide(-2));
        label.translateYProperty().bind(label.heightProperty().divide(-2));
        name.addListener((obs, oldVal, newVal) -> label.setText(newVal));
        layoutXProperty().addListener((obs, oldVal, newVal) -> label.setLayoutX(newVal.doubleValue()));
        layoutYProperty().addListener((obs, oldVal, newVal) -> label.setLayoutY(30 + newVal.doubleValue()));
        label.setLayoutX(getLayoutX());
        label.setLayoutY(30 + getLayoutY());
    }

    public double getPrevLayoutX() {
        return prevLayoutX;
    }

    public double getPrevLayoutY() {
        return prevLayoutY;
    }

    public void setPrevLayout(double x, double y) {
        prevLayoutX = x;
        prevLayoutY = y;
    }

    public Label getLabel() {
        return label;
    }

    public List<Node> asChild() {
        return List.of(this, label);
    }


    public ObservableList<Arc> getInArcs() {
        return inArcs;
    }

    public ObservableList<Arc> getOutArcs() {
        return outArcs;
    }

    public List<Arc> getAdjacentArcs() {
        return Stream.concat(inArcs.stream(), outArcs.stream()).toList();
    }

    public String getGraphId() {
        return graphId;
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public abstract BaseVertex copy();

    public void setSelectedStyle() {
        label.setOpacity(0.5);
        setOpacity(0.5);
    };

    public void setUnselectedStyle() {
        label.setOpacity(1);
        setOpacity(1);
    };

}
