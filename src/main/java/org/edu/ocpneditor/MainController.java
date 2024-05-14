package org.edu.ocpneditor;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.edu.ocpneditor.algos.PetriValidation;
import org.edu.ocpneditor.utils.GraphActionManager;
import org.edu.ocpneditor.utils.GraphChange;
import org.edu.ocpneditor.view.PropertyPaneManager;
import org.edu.ocpneditor.fileparser.OCNetDeserializer;
import org.edu.ocpneditor.fileparser.OCNetSerializer;
import org.edu.ocpneditor.panemodels.*;
import org.edu.ocpneditor.petriobj.PetriTypeStageHandler;
import org.edu.ocpneditor.petriobj.PetriType;
import org.edu.ocpneditor.algos.GraphLayout;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;


public class MainController implements Initializable {

    @FXML
    public VBox elemProperties;

    @FXML
    public ScrollPane graphScroll;

    @FXML
    public AnchorPane graphResizePane;

    @FXML
    private AnchorPane graph;

    @FXML
    private ToggleButton arcMode;

    @FXML
    private ToggleButton mouseMode;

    @FXML
    private ToggleButton placeMode;

    @FXML
    private ToggleGroup placementModes;

    @FXML
    private ToggleButton transitionMode;

    private final ObservableList<Selectable> selected = FXCollections.observableArrayList();
    private final ObservableList<BaseVertex> copyBuffer = FXCollections.observableArrayList();
    private BaseVertex source;
    private Line sampleArc;
    private PropertyPaneManager propertyManager;

    private GraphActionManager actions;

    private final Scale scaleTransform = new Scale(1, 1);

    List<PetriType> petriTypes = new ArrayList<>();

    private double cursorX;
    private double cursorY;

    // Actions before application starts
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        placementModes.selectedToggleProperty().addListener((obsVal, oldVal, newVal) -> {
            if (newVal == null)
                oldVal.setSelected(true);
        });
        petriTypes.add(PetriType.getDefaultType());
        selected.addListener((ListChangeListener<? super Selectable>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Selectable element : change.getAddedSubList()) {
                        setElementContextMenu(element);
                        element.setSelectedStyle();
                    }
                }
                if (change.wasRemoved()) {
                    for (Selectable element : change.getRemoved()) {
                        removeElementContextMenu(element);
                        element.setUnselectedStyle();
                    }
                }
                propertyManager.selectionChanged(selected);
            }
        });
        graphScroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                onGraphPaneScroll(event);
                event.consume();
            }
        });
        graph.setOnMouseMoved(e -> {
            cursorX = e.getX();
            cursorY = e.getY();
        });
        graph.getTransforms().add(scaleTransform);
        initGraphContextMenu();
        actions = new GraphActionManager(graph, petriTypes);
        propertyManager = new PropertyPaneManager(elemProperties, graph, actions, petriTypes);
    }

    // Graphical graph manipulations
    @FXML
    public void onGraphPanePressed(MouseEvent event) {
        if (event.isPrimaryButtonDown()) {
            if (mouseMode.isSelected()) {
                // Very doubtful. It's checks if target is pane, because when arc is clicked what event mistakenly appears
                if (event.getTarget() == graph) selected.clear();
            }
            if (placeMode.isSelected()) {
                actions.execute(addVertex(new Place(event.getX(), event.getY())));
            }
            if (transitionMode.isSelected()) {
                actions.execute(addVertex(new Transition(event.getX(), event.getY())));
            }
        }
    }

    @FXML
    public void onGraphPaneDragged(MouseEvent event) {
        if (arcMode.isSelected() && sampleArc != null) {
            sampleArc.setEndX(event.getX());
            sampleArc.setEndY(event.getY());
        } else if (!selected.isEmpty()) {
            for (Selectable selection : selected) {
                if (selection instanceof BaseVertex vertex) {
                    vertex.setLayoutX(event.getX());
                    vertex.setLayoutY(event.getY());
                }
            }
        }
    }

    @FXML
    public void appKeyAction(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.DELETE)) {
            actions.executeAll(deleteAllElements(selected));
            selected.clear();
        }
        if (keyEvent.isControlDown()) {
            switch (keyEvent.getCode()) {
                case V:
                    if (keyEvent.isAltDown() && !copyBuffer.isEmpty()) {
                        pasteAction(cursorX - copyBuffer.get(copyBuffer.size() - 1).getLayoutX(),
                                cursorY - copyBuffer.get(copyBuffer.size() - 1).getLayoutY());
                    } else {
                        pasteAction(0, 0);
                    }
                    break;
                case Z:
                    actions.undo();
                    break;
                case Y:
                    actions.redo();
                    break;
            }
            selected.clear();
        }
    }

    // Context Menus

    private void initGraphContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem newPlace = new MenuItem("New place");
        MenuItem newTransition = new MenuItem("New transition");
        MenuItem paste = new MenuItem("Paste");
        paste.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
        paste.setVisible(false);
        MenuItem pasteHere = new MenuItem("Paste here");
        pasteHere.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN));
        pasteHere.setVisible(false);
        copyBuffer.addListener((ListChangeListener<? super Selectable>) change -> {
            paste.setVisible(!copyBuffer.isEmpty());
            pasteHere.setVisible(!copyBuffer.isEmpty());
        });
        menu.getItems().addAll(newPlace, newTransition, paste, pasteHere);

        graph.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            newPlace.setOnAction(e -> actions.execute(addVertex(new Place(event.getX(), event.getY()))));
            newTransition.setOnAction(e -> actions.execute(addVertex(new Transition(event.getX(), event.getY()))));
            paste.setOnAction(e -> pasteAction(0, 0));
            pasteHere.setOnAction(e -> pasteAction(event.getX() - copyBuffer.get(copyBuffer.size() - 1).getLayoutX(),
                    event.getY() - copyBuffer.get(copyBuffer.size() - 1).getLayoutY()));
            menu.show(graph, event.getScreenX(), event.getScreenY());
            event.consume();
        });
        graph.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> menu.hide());
    }

    private void pasteAction(double shiftX, double shiftY) {
        if (copyBuffer.isEmpty()) return;
        actions.execute(actions.pasteGraphElements(List.copyOf(copyBuffer)));
        copyBuffer.forEach(vertex -> {
            initVertex(vertex);
            vertex.getAdjacentArcs().forEach(this::initArc);
            vertex.setLayoutX(vertex.getLayoutX() + shiftX);
            vertex.setLayoutY(vertex.getLayoutY() + shiftY);
        });
        List<BaseVertex> copies = new ArrayList<>(copyBuffer);
        copyBuffer.clear();
        copyBuffer.addAll(copySelection(copies));
    }

    private void removeElementContextMenu(Selectable selection) {
        if (selection instanceof BaseVertex vertex) {
            vertex.setContextMenu(null);
        } else if (selection instanceof Arc arc) {
            arc.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            });
            arc.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            });
        }
    }

    private void setElementContextMenu(Selectable selection) {
        ContextMenu menu = new ContextMenu();
        MenuItem deleteElement = new MenuItem("Delete");
        deleteElement.setOnAction(e -> {
            actions.executeAll(deleteAllElements(selected));
            selected.clear();
            menu.hide();
        });
        menu.getItems().add(deleteElement);
        if (selection instanceof BaseVertex vertex) {
            MenuItem cutVertex = new MenuItem("Cut");
            cutVertex.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
            MenuItem copyVertex = new MenuItem("Copy");
            copyVertex.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
            cutVertex.setOnAction(e -> {
                copyBuffer.clear();
                for (Selectable select : selected) {
                    if (select instanceof BaseVertex vertex1) {
                        copyBuffer.add(vertex1);
                    }
                }
                actions.execute(actions.cutGraphElements(List.copyOf(copyBuffer), List.copyOf(selected)));
            });
            copyVertex.setOnAction(e -> {
                copyBuffer.clear();
                List<BaseVertex> toCopy = new ArrayList<>();
                for (Selectable select : selected) {
                    if (select instanceof BaseVertex vertex1) {
                        toCopy.add(vertex1);
                    }
                }
                copyBuffer.addAll(copySelection(toCopy));
            });
            menu.getItems().addAll(cutVertex, copyVertex);
            vertex.setContextMenu(menu);
        } else if (selection instanceof Arc arc) {
            MenuItem addPoint = new MenuItem("Add point");
            menu.getItems().add(addPoint);
            arc.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
                if (event.getTarget() instanceof ArcPoint point) {
                    deleteElement.setOnAction(e -> {
                        actions.execute(actions.deleteArcPoint(arc, point));
                        menu.hide();
                    });
                    menu.getItems().remove(addPoint);
                } else {
                    addPoint.setOnAction(action -> {
                        ArcPoint point = new ArcPoint(arc, event.getX(), event.getY());
                        actions.execute(actions.addArcPoint(arc, point));
                        point.setOnMousePressed(e -> onArcPointPressed(e, arc, point));
                        point.setOnDragDetected(e -> onArcPointDragDetected(e, point));
                        point.setOnMouseDragged(e -> onArcPointDragged(e, point));
                        point.setOnMouseReleased(e -> onArcPointReleased(e, point));
                        menu.hide();
                    });
                }
                menu.show(arc, event.getScreenX(), event.getScreenY());
                event.consume();
            });
            arc.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> menu.hide());
        }
    }

    private List<BaseVertex> copySelection(List<BaseVertex> toCopy) {
        Map<BaseVertex, BaseVertex> copies = new HashMap<>();
        for (BaseVertex vertex : toCopy) {
            BaseVertex copy = vertex.copy();
            copies.put(vertex, copy);
        }
        for (BaseVertex vertex : copies.keySet()) {
            for (Arc arc : vertex.getInArcs()) {
                BaseVertex from = arc.getSource();
                if (copies.containsKey(from)) {
                    Arc copyArc = new Arc(copies.get(from), copies.get(vertex));
                    copies.get(from).getOutArcs().add(copyArc);
                    copies.get(vertex).getInArcs().add(copyArc);

                }
            }
            for (Arc arc : vertex.getOutArcs()) {
                BaseVertex to = arc.getTarget();
                if (copies.containsKey(to)) {
                    Arc copyArc = new Arc(copies.get(vertex), copies.get(to));
                    copies.get(vertex).getOutArcs().add(copyArc);
                    copies.get(to).getInArcs().add(copyArc);
                }
            }
        }
        return List.copyOf(copies.values());
    }

    // Vertices and arcs events
    private void onVertexDragDetected(MouseEvent event, BaseVertex vertex) {
        vertex.toFront();
        if (mouseMode.isSelected()) {
            vertex.setPrevLayout(vertex.getLayoutX(), vertex.getLayoutY());
        }
        if (arcMode.isSelected()) {
            if (vertex instanceof Place && ((Place) vertex).isEndPlace()) return;
            source = vertex;
            sampleArc = new Line(vertex.getLayoutX(), vertex.getLayoutY(), event.getX(), event.getY());
            sampleArc.setStroke(Color.GRAY);
            graph.getChildren().add(sampleArc);
        }
    }

    private void onVertexDragged(MouseEvent event, BaseVertex vertex) {
        if (arcMode.isSelected() && sampleArc != null) {
            sampleArc.setEndX(vertex.getLayoutX() + event.getX() + vertex.getTranslateX());
            sampleArc.setEndY(vertex.getLayoutY() + event.getY() + vertex.getTranslateY());
        }
        if (mouseMode.isSelected()) {
            vertex.setLayoutX(vertex.getLayoutX() + event.getX() + vertex.getTranslateX());
            vertex.setLayoutY(vertex.getLayoutY() + event.getY() + vertex.getTranslateY());
        }
    }

    private void onVertexReleased(MouseEvent e, BaseVertex vertex) {
        if (mouseMode.isSelected()) {
            if (vertex.getPrevLayoutX() == vertex.getLayoutX() &&
                    vertex.getPrevLayoutY() == vertex.getLayoutY()) return;
            actions.addGraphAction(actions.moveGraphVertex(vertex, vertex.getLayoutX(), vertex.getLayoutY(),
                    vertex.getPrevLayoutX(), vertex.getPrevLayoutY()));

        }
        if (arcMode.isSelected() && sampleArc != null) {
            Optional<Node> target = graph.getChildren().stream().filter(el ->
                    el.getBoundsInParent().getMinX() <= sampleArc.getEndX() &&
                            el.getBoundsInParent().getMaxX() >= sampleArc.getEndX() &&
                            el.getBoundsInParent().getMinY() <= sampleArc.getEndY() &&
                            el.getBoundsInParent().getMaxY() >= sampleArc.getEndY() &&
                            el instanceof BaseVertex).findAny();
            if (target.isPresent() && (target.get() instanceof BaseVertex)) {
                Arc arc = new Arc(source, (BaseVertex) target.get());
                source.getOutArcs().add(arc);
                ((BaseVertex) target.get()).getInArcs().add(arc);
                GraphChange addArc = addArrow(arc);
                if (addArc != null) actions.execute(addArc);
            }
            graph.getChildren().remove(sampleArc);
            sampleArc = null;
        }
    }

    private void onVertexPressed(MouseEvent event, BaseVertex vertex) {
        if (mouseMode.isSelected()) {
            if (!event.isControlDown()) {
                selected.clear();
            }
            selectGraphElement(vertex);
        }
    }

    private void onArcPressed(MouseEvent event, Arc arc) {
        if (mouseMode.isSelected()) {
            if (!event.isControlDown()) {
                selected.clear();
            }
            selectGraphElement(arc);
        }
    }

    private void onArcPointPressed(MouseEvent event, Arc arc, ArcPoint point) {
        if (mouseMode.isSelected()) {
            selectGraphElement(arc);
        }
    }

    private void onArcPointDragDetected(MouseEvent event, ArcPoint point) {
        point.toFront();
        if (mouseMode.isSelected()) {
            point.setPrevLayout(point.getLayoutX(), point.getLayoutY());
        }
    }

    private void onArcPointDragged(MouseEvent event, ArcPoint point) {
        if (mouseMode.isSelected()) {
            point.setLayoutX(point.getLayoutX() + event.getX() + point.getTranslateX());
            point.setLayoutY(point.getLayoutY() + event.getY() + point.getTranslateY());
        }
    }

    private void onArcPointReleased(MouseEvent e, ArcPoint point) {
        if (mouseMode.isSelected()) {
            if (point.getPrevLayoutX() == point.getLayoutX() &&
                    point.getPrevLayoutY() == point.getLayoutY()) return;
            actions.addGraphAction(actions.moveArcPoint(point, point.getLayoutX(), point.getLayoutY(),
                    point.getPrevLayoutX(), point.getPrevLayoutY()));

        }
    }

    // helper methods
    private void selectGraphElement(Node newSelection) {
        if (newSelection instanceof Selectable currentSelection) {
            selected.add(currentSelection);
        } else {
            selected.clear();
        }
    }

    private void initVertex(BaseVertex vertex) {
        vertex.setOnDragDetected(e -> onVertexDragDetected(e, vertex));
        vertex.setOnMouseDragged(e -> onVertexDragged(e, vertex));
        vertex.setOnMouseReleased(e -> onVertexReleased(e, vertex));
        vertex.setOnMousePressed(e -> onVertexPressed(e, vertex));
    }

    private GraphChange addVertex(BaseVertex vertex) {
        vertex.setOnDragDetected(e -> onVertexDragDetected(e, vertex));
        vertex.setOnMouseDragged(e -> onVertexDragged(e, vertex));
        vertex.setOnMouseReleased(e -> onVertexReleased(e, vertex));
        vertex.setOnMousePressed(e -> onVertexPressed(e, vertex));
        selected.clear();
        selected.add(vertex);
        return actions.addGraphElement(vertex);
    }

    private List<GraphChange> addAllVertices(List<BaseVertex> vertices) {
        List<GraphChange> addChanges = new ArrayList<>();
        vertices.forEach(vertex -> addChanges.add(addVertex(vertex)));
        return addChanges;
    }

    private GraphChange deleteElement(Selectable selection) {
        return actions.deleteGraphElement(selection);
    }

    private List<GraphChange> deleteAllElements(List<Selectable> selections) {
        List<GraphChange> deleteChanges = new ArrayList<>();
        selections.forEach(selection -> deleteChanges.add(deleteElement(selection)));
        return deleteChanges;
    }

    private void initArc(Arc arc) {
        BaseVertex from = arc.getSource();
        BaseVertex to = arc.getTarget();

        arc.startXProperty().bind(from.layoutXProperty());
        arc.startYProperty().bind(from.layoutYProperty());
        arc.endXProperty().bind(to.layoutXProperty());
        arc.endYProperty().bind(to.layoutYProperty());

        arc.setOnMousePressed(e -> onArcPressed(e, arc));
    }

    private GraphChange addArrow(Arc arc) {

        BaseVertex from = arc.getSource();
        BaseVertex to = arc.getTarget();

        if (from instanceof Place && to instanceof Place || from instanceof Transition && to instanceof Transition) {
            return null;
        }
        if (graph.getChildren().contains(arc)) return null;

        initArc(arc);
        selected.clear();
        selected.add(arc);

        return actions.addGraphElement(arc);
    }

    @FXML
    public void saveAsOCDotMode(ActionEvent actionEvent) {
        OCNetSerializer serializer = new OCNetSerializer(graph, petriTypes);
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("OCDot Files", "*.ocdot"));
            File created = fileChooser.showSaveDialog(new Stage());
            serializer.serializeModel(created);
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("File save error");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void saveNetConfigurationAsYaml(ActionEvent actionEvent) {
        OCNetSerializer serializer = new OCNetSerializer(graph, petriTypes);
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Yaml Files", "*.yaml"));
            File created = fileChooser.showSaveDialog(new Stage());
            serializer.serializeConfiguration(created);
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("File save error");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void arrangeGraph(ActionEvent actionEvent) {
        GraphLayout layoutSetter = new GraphLayout(graph);
        actions.executeAll(layoutSetter.arrange(actions));
    }

    @FXML
    public void validationAction(ActionEvent actionEvent) {
        PetriValidation validator = new PetriValidation(graph);
        validator.validation();
    }

    @FXML
    public void selectDefaultPlaceType(ActionEvent actionEvent) {
        try {
            PetriTypeStageHandler openStage = new PetriTypeStageHandler(graph, petriTypes);
            PetriType selectedType = openStage.selectAndUpdatePlaceType(null);
            PetriType.setDefaultType(selectedType);
        } catch (IOException ex) {
            System.out.println("OPEN FXML FILE EXCEPTION: " + ex.getMessage());
        }
    }

    @FXML
    public void downloadNetFromOCDotAndYaml(ActionEvent actionEvent) {
        OCNetDeserializer deserializer = new OCNetDeserializer(graph);
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("OCDot Files", "*.ocdot"));
            File created = fileChooser.showOpenDialog(new Stage());
            deserializer.deserializeModel(created);
            fileChooser.getExtensionFilters().clear();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Yaml Files", "*.yaml"));
            created = fileChooser.showOpenDialog(new Stage());
            deserializer.deserializeConfiguration(created);
            graph.getChildren().clear();
            petriTypes.clear();
            for (BaseVertex vertex : deserializer.getVertices()) {
                actions.execute(addVertex(vertex));
            }
            for (Arc arc : deserializer.getArcs()) {
                GraphChange addArc = addArrow(arc);
                arc.getSource().getOutArcs().add(arc);
                arc.getTarget().getInArcs().add(arc);
                arc.updateColor();
                if (addArc != null) actions.execute(addArc);
            }
            if (!deserializer.getTypes().isEmpty()) {
                actions.clear();
                PetriType.setDefaultType(deserializer.getTypes().get(0));
                petriTypes.addAll(deserializer.getTypes());
            } else {
                PetriType.setEmptyDefault();
                petriTypes.add(PetriType.getDefaultType());
            }
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("File import error");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
        arrangeGraph(actionEvent);
    }

    @FXML
    public void onGraphPaneScroll(ScrollEvent scrollEvent) {
        final double MIN_SCALE = 0.15;
        final double MAX_SCALE = 1.85;
        if (scrollEvent.isControlDown()) {
            double delta = 0.02;
            double scale = scaleTransform.getY() + (scrollEvent.getDeltaY() > 0 ? delta : -delta);
            if (scale < MIN_SCALE || scale > MAX_SCALE) return;
            graph.setLayoutX(0);
            graph.setLayoutY(0);
            scaleTransform.setX(scale);
            scaleTransform.setY(scale);
            graphResizePane.setPrefWidth(graph.getPrefWidth() * scale);
            graphResizePane.setPrefHeight(graph.getPrefHeight() * scale);
        }
    }


    @FXML
    public void undoAction(MouseEvent event) {
        actions.undo();
    }

    @FXML
    public void redoAction(MouseEvent event) {
        actions.redo();
    }
}