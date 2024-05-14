package org.edu.ocpneditor.panemodels;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;

import java.util.ArrayList;
import java.util.List;

public class Arc extends Group implements Selectable {

    private final double LENGTH_SCALER = 20;
    private final double ARCHEAD_ANGLE = Math.toRadians(20);
    private final double ARCHEAD_LENGHT = 15;

    private static Paint varArcVanish = Color.LIGHTGRAY;
    private final Polyline mainLine = new Polyline();
    private final Polyline variableLine = new Polyline();
    private final Polygon arcHead = new Polygon();
    private final SimpleDoubleProperty startX = new SimpleDoubleProperty();
    private final SimpleDoubleProperty startY = new SimpleDoubleProperty();
    private final SimpleDoubleProperty endX = new SimpleDoubleProperty();
    private final SimpleDoubleProperty endY = new SimpleDoubleProperty();

    private final List<ArcPoint> extraPoint = new ArrayList<>();

    private final BaseVertex source;
    private final BaseVertex target;

    private SimpleIntegerProperty weight = new SimpleIntegerProperty(1);

    private Label weightLabel = new Label();
    private SimpleBooleanProperty isVariable = new SimpleBooleanProperty(false);
    private boolean isTwoSided = false;

    public Arc(BaseVertex source, BaseVertex target) {

        this.mainLine.setStrokeWidth(1.5);
        this.variableLine.setStrokeWidth(0);
        this.variableLine.setStroke(Color.TRANSPARENT);
        this.arcHead.setStrokeWidth(1.5);
        this.source = source;
        this.target = target;

        startX.set(source.getLayoutX());
        startY.set(source.getLayoutY());
        endX.set(target.getLayoutX());
        endY.set(target.getLayoutY());

        initWeightLabel();
        getChildren().addAll(mainLine, variableLine, arcHead);

        for (SimpleDoubleProperty cord : new SimpleDoubleProperty[]{startX, startY, endX, endY}) {
            cord.addListener((l, o, n) -> updateArc());
        }

        updateArc();
        updateColor();
    }

    private void initWeightLabel() {
        weightLabel.translateXProperty().bind(weightLabel.widthProperty().divide(-2));
        weightLabel.translateYProperty().bind(weightLabel.heightProperty().divide(-2));
        startX.addListener((observable -> setWeightLabelLayout()));
        startY.addListener((observable -> setWeightLabelLayout()));
        endX.addListener((observable -> setWeightLabelLayout()));
        endY.addListener((observable -> setWeightLabelLayout()));
        setWeightLabelLayout();
        weightLabel.setBackground(Background.fill(varArcVanish));

        weight.addListener((obs, oldVal, newVal) -> {
            if (weight.get() == 1) weightLabel.setText("");
            if (weight.get() != 1 && !isVariable()) weightLabel.setText(newVal.toString());
        });
        isVariable.addListener((obs, oldVal, newVal) -> {
            if (newVal || weight.get() == 1) weightLabel.setText("");
            else weightLabel.setText(Integer.toString(weight.get()));
        });
    }

    private void setWeightLabelLayout() {
        weightLabel.setLayoutX((startX.get() + endX.get()) / 2);
        weightLabel.setLayoutY((startY.get() + endY.get()) / 2);
    }

    private void updateArc() {
        double nextX, nextY;
        if (extraPoint.isEmpty()) {
            nextX = endX.get();
            nextY = endY.get();
        } else {
            nextX = extraPoint.get(0).getLayoutX();
            nextY = extraPoint.get(0).getLayoutY();
        }
        double[] startLine = scale(startX.get(), startY.get(), nextX, nextY, LENGTH_SCALER + 10);
        mainLine.getPoints().setAll(startLine[0], startLine[1]);
        variableLine.getPoints().setAll(startLine[0], startLine[1]);
        for (ArcPoint point : extraPoint) {
            mainLine.getPoints().addAll(point.getLayoutX(), point.getLayoutY());
            variableLine.getPoints().addAll(point.getLayoutX(), point.getLayoutY());
        }
        double lastX, lastY;
        if (extraPoint.isEmpty()) {
            lastX = startLine[0];
            lastY = startLine[1];
        } else {
            lastX = extraPoint.get(extraPoint.size() - 1).getLayoutX();
            lastY = extraPoint.get(extraPoint.size() - 1).getLayoutY();
        }
        updateArcHead(lastX, lastY);
    }

    private void updateArcHead(double prevX, double prevY) {
        double[] end = scale(endX.get(), endY.get(), prevX, prevY, LENGTH_SCALER);
        double[] endLine = scale(endX.get(), endY.get(), prevX, prevY, LENGTH_SCALER + 10);

        mainLine.getPoints().addAll(endLine[0], endLine[1]);
        variableLine.getPoints().addAll(endLine[0], endLine[1]);

        double angle = Math.atan2(end[1] - prevY, end[0] - prevX);

        double x = end[0] - Math.cos(angle + ARCHEAD_ANGLE) * ARCHEAD_LENGHT;
        double y = end[1] - Math.sin(angle + ARCHEAD_ANGLE) * ARCHEAD_LENGHT;
        arcHead.getPoints().setAll(x, y, end[0], end[1]);

        x = end[0] - Math.cos(angle - ARCHEAD_ANGLE) * ARCHEAD_LENGHT;
        y = end[1] - Math.sin(angle - ARCHEAD_ANGLE) * ARCHEAD_LENGHT;
        arcHead.getPoints().addAll(x, y);
    }

    private double[] scale(double x1, double y1, double x2, double y2, double scaler) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        return new double[]{
                x1 + Math.cos(angle) * scaler,
                y1 + Math.sin(angle) * scaler
        };
    }

    public void addPoint(ArcPoint point) {
        if (!insertPoint(point)) return;
        getChildren().add(point);
        for (DoubleProperty cord : new DoubleProperty[]{point.layoutXProperty(), point.layoutYProperty()}) {
            cord.addListener((l, o, n) -> updateArc());
        }
        updateArc();
    }

    public void removePoint(ArcPoint point) {
        extraPoint.remove(point);
        getChildren().remove(point);
        updateArc();
    }

    public void clearPoints() {
        getChildren().removeAll(extraPoint);
        extraPoint.clear();
    }

    private boolean insertPoint(ArcPoint point) {
        if (extraPoint.isEmpty()) {
            extraPoint.add(point);
            return true;
        }
        int insertIndex = -1;
        if (isTheSameVector(startX.get(), startY.get(),
                extraPoint.get(0).getLayoutX(), extraPoint.get(0).getLayoutY(), point)) {
            insertIndex = 0;
        }
        for (int i = 1; i < extraPoint.size(); ++i) {
            if (isTheSameVector(extraPoint.get(i - 1).getLayoutX(), extraPoint.get(i - 1).getLayoutY(),
                    extraPoint.get(i).getLayoutX(), extraPoint.get(i).getLayoutY(), point)) {
                insertIndex = i;
            }
        }
        if (isTheSameVector(extraPoint.get(extraPoint.size() - 1).getLayoutX(),
                extraPoint.get(extraPoint.size() - 1).getLayoutY(), endX.get(), endY.get() , point)) {
            insertIndex = extraPoint.size();
        }
        if (insertIndex > -1) {
            extraPoint.add(insertIndex, point);
            return true;
        } else {
            return false;
        }
    }

    private boolean isTheSameVector(double startX, double startY, double endX, double endY, ArcPoint point) {
        double epsilon = 0.02;
        double v1X = endX - startX;
        double v1Y = endY - startY;
        double v2X = point.getLayoutX() - startX;
        double v2Y = point.getLayoutY() - startY;

        double lenV1 = Math.sqrt(v1X * v1X + v1Y * v1Y);
        double lenV2 = Math.sqrt(v2X * v2X + v2Y * v2Y);

        double normV1X = v1X / lenV1;
        double normV1Y = v1Y / lenV1;
        double normV2X = v2X / lenV2;
        double normV2Y = v2Y / lenV2;

        return Math.abs(normV1X - normV2X) < epsilon && Math.abs(normV1Y - normV2Y) < epsilon;
    }

    public void removePoint(double x, double y) {
        mainLine.getPoints().removeAll(x, y);
    }

    public boolean isTwoSided() {
        return isTwoSided;
    }

    public void setTwoSided(boolean twoSided) {
        isTwoSided = twoSided;

    }

    public static void setVarArcVanish(Paint color) {
        varArcVanish = color;
    }

    public double getStartX() {
        return startX.get();
    }

    public SimpleDoubleProperty startXProperty() {
        return startX;
    }

    public void setStartX(double startX) {
        this.startX.set(startX);
    }

    public double getStartY() {
        return startY.get();
    }

    public SimpleDoubleProperty startYProperty() {
        return startY;
    }

    public void setStartY(double startY) {
        this.startY.set(startY);
    }

    public double getEndX() {
        return endX.get();
    }

    public SimpleDoubleProperty endXProperty() {
        return endX;
    }

    public void setEndX(double endX) {
        this.endX.set(endX);
    }

    public double getEndY() {
        return endY.get();
    }

    public SimpleDoubleProperty endYProperty() {
        return endY;
    }

    public void setEndY(double endY) {
        this.endY.set(endY);
    }

    public int getWeight() {
        return weight.get();
    }

    public void setWeight(int weight) {
        this.weight.set(weight);
    }

    public boolean isVariable() {
        return isVariable.get();
    }

    public void setVariable(boolean variable) {
        isVariable.set(variable);
        if (variable) {
            variableLine.setStroke(varArcVanish);
            variableLine.setStrokeWidth(mainLine.getStrokeWidth());
            mainLine.setStrokeWidth(mainLine.getStrokeWidth() * 3);
        } else {
            variableLine.setStroke(Color.TRANSPARENT);
            variableLine.setStrokeWidth(0);
            mainLine.setStrokeWidth(mainLine.getStrokeWidth() / 3);
        }
        variableLine.toBack();
        mainLine.toBack();
    }

    public BaseVertex getSource() {
        return source;
    }

    public BaseVertex getTarget() {
        return target;
    }

    public void updateColor() {
        Place place = source instanceof Place ? (Place) source : (Place) target;
        setTypeColor(place.getType().getColor());
    }

    public void setTypeColor(Paint color) {
        mainLine.setStroke(color);
        arcHead.setFill(color);
    }

    @Override
    public void setSelectedStyle() {
        extraPoint.forEach(ArcPoint::setSelectedStyle);
        setOpacity(0.5);
    }

    @Override
    public void setUnselectedStyle() {
        extraPoint.forEach(ArcPoint::setUnselectedStyle);
        setOpacity(1);
    }

    @Override
    public List<Node> asChild() {
        return List.of(this, weightLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Arc)) return false;
        Arc arc = (Arc) o;
        return (arc == this) || (this.target.equals(arc.target) && this.source.equals(arc.source));
    }

}
