package org.edu.ocpneditor.view;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.edu.ocpneditor.panemodels.*;
import org.edu.ocpneditor.petriobj.PetriTypeStageHandler;
import org.edu.ocpneditor.petriobj.PetriType;
import org.edu.ocpneditor.utils.GraphActionManager;
import org.edu.ocpneditor.utils.GraphChange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PropertyPaneManager {

    private final VBox propertyField;
    private final AnchorPane graph;
    private final GraphActionManager actions;
    private final List<PetriType> petriTypes;

    public PropertyPaneManager(VBox propertyPane, AnchorPane graph, GraphActionManager actions, List<PetriType> objects) {
        super();
        this.actions = actions;
        petriTypes = objects;
        propertyField = propertyPane;
        propertyField.setPadding(new Insets(50, 20, 10, 20));
        propertyField.setAlignment(Pos.TOP_CENTER);
        propertyField.setSpacing(5);
        this.graph = graph;
    }

    public void selectionChanged(ObservableList<Selectable> selection) {
        propertyField.getChildren().clear();
        if (selection.isEmpty()) return;
        if (selection.size() == 1) {
            if (selection.get(0) instanceof Arc arc) {
                addArcProperties(arc);
            }
            if (selection.get(0) instanceof BaseVertex vertex) {
                addVertexProperties(vertex);
            }
        }
    }

    private void clear() {
        propertyField.getChildren().clear();
    }

    private void addArcProperties(Arc arc) {
        Label source = new Label("Source: ID " + arc.getSource().getGraphId() + ", " + arc.getSource().getName());
        Label target = new Label("Target: ID " + arc.getTarget().getGraphId() + ", " + arc.getTarget().getName());

        TextField weightField = new TextField(Integer.toString(arc.getWeight()));
        weightField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                int newWeight = newValue.isEmpty() ? 1 : Integer.parseInt(newValue);
                if (newWeight < 1) {
                    weightField.setText(oldValue);
                    return;
                }
            } catch (NumberFormatException ex) {
                weightField.setText(oldValue);
            }
        });
        weightField.onKeyReleasedProperty();

        CheckBox isVariable = new CheckBox("Variable arc");
        isVariable.setSelected(arc.isVariable());

        Button apply = new Button("Apply");
        apply.setOnMouseClicked(e -> {
            List<GraphChange> changes = new ArrayList<>();
            if (safeParseWeight(weightField.getText()) != arc.getWeight()) {
                changes.add(actions.updateArcWeight(arc, arc.getWeight(), safeParseWeight(weightField.getText())));
            }
            if (arc.isVariable() != isVariable.isSelected()) {
                changes.add(actions.setVariable(arc, arc.isVariable(), isVariable.isSelected()));
            }
            actions.executeAll(changes);
        });

        propertyField.getChildren().addAll(source, target, weightField, isVariable, apply);
    }

    private void addVertexProperties(BaseVertex vertex) {

        Label idLabel = new Label("ID: " + vertex.getGraphId());

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField(vertex.getName());
        // nameField.textProperty().addListener((observable, oldValue, newValue) -> vertex.setName(newValue));

        Button apply = new Button("Apply");

        apply.setOnMouseClicked(e -> {
            actions.execute(actions.setNameProperty(vertex, vertex.getName(), nameField.getText()));
        });

        if (!(vertex instanceof Place)) {
            propertyField.getChildren().addAll(idLabel, nameLabel, nameField, apply);
            return;
        }

        Place place = (Place) vertex;
        Label typeLabel = new Label("Type: " + place.getType().getName());
        Button openObjectPicker = new Button("     Select type     ");
        openObjectPicker.setOnMouseClicked(e -> {
            actions.execute(actions.updatePlaceType(place, petriTypes, place.getType(), getNewPlaceType(place)));
            typeLabel.setText("Type: " + place.getType().getName());
        });

        Separator separator = new Separator();

        Label tokenLabel = new Label("Start tokens");
        TextField startTokens = new TextField(Integer.toString(place.getStartTokens()));
        startTokens.setDisable(place.isEndPlace());
        startTokens.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                int newTokens = newValue.isEmpty() ? 0 : Integer.parseInt(newValue);
                if (newTokens < 0) {
                    startTokens.setText(oldValue);
                }
            } catch (NumberFormatException ex) {
                startTokens.setText(oldValue);
            }
        });

        CheckBox isEndPlace = new CheckBox("End position");
        isEndPlace.setDisable(!place.getOutArcs().isEmpty());
        isEndPlace.setSelected(place.isEndPlace());
        isEndPlace.selectedProperty().addListener((observableValue, oldValue, newValue) -> {
            startTokens.setDisable(newValue);
            if (newValue) {
                startTokens.setText("");
            }
        });

        apply.setOnMouseClicked(e -> {
            List<GraphChange> changes = new ArrayList<>();
            if (!vertex.getName().equals(nameField.getText())) {
                changes.add(actions.setNameProperty(vertex, vertex.getName(), nameField.getText()));
            }
            if (place.getStartTokens() != safeParseTokens(startTokens.getText())) {
                changes.add(actions.updateTokens(place, place.getStartTokens(), safeParseTokens(startTokens.getText())));
            }
            if (place.isEndPlace() != isEndPlace.isSelected()) {
                changes.add(actions.setEndPlace(place, place.isEndPlace(), isEndPlace.isSelected()));
            }
            actions.executeAll(changes);
        });

        propertyField.getChildren().addAll(idLabel, nameLabel, nameField, separator,
                tokenLabel, startTokens, isEndPlace, apply, typeLabel, openObjectPicker);
    }

    private int safeParseTokens(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private int safeParseWeight(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private PetriType getNewPlaceType(Place place) {
        try {
            PetriTypeStageHandler openStage = new PetriTypeStageHandler(graph, petriTypes);
            return openStage.selectAndUpdatePlaceType(place);
        } catch (IOException ex) {
            System.out.println("OPEN FXML FILE EXCEPTION: " + ex.getMessage());
            return null;
        }
    }
}
