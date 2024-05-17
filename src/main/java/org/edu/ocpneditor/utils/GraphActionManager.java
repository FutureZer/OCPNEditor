package org.edu.ocpneditor.utils;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import org.edu.ocpneditor.panemodels.*;
import org.edu.ocpneditor.petriobj.PetriType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class GraphActionManager {

    AnchorPane graph;

    List<PetriType> types;

    private final Deque<List<GraphChange>> undoQueue = new ArrayDeque<>();
    private final Deque<List<GraphChange>> redoQueue = new ArrayDeque<>();

    public GraphActionManager(AnchorPane graph, List<PetriType> types) {
        this.types = types;
        this.graph = graph;
    }

    public GraphChange addGraphElement(Selectable element) {
        return new GraphChange() {
            @Override
            public void execute() {
                addAction(element);
            }

            @Override
            public void undo() {
                deleteAction(element);
            }
        };
    }

    public GraphChange addArcPoint(Arc arc, ArcPoint point) {
        return new GraphChange() {
            @Override
            public void execute() {
                arc.addPoint(point);
            }

            @Override
            public void undo() {
                arc.removePoint(point);
            }
        };
    }

    public GraphChange deleteArcPoint(Arc arc, ArcPoint point) {
        return new GraphChange() {
            @Override
            public void execute() {
                arc.removePoint(point);
            }

            @Override
            public void undo() {
                arc.addPoint(point);
            }
        };
    }

    public GraphChange deleteGraphElement(Selectable element) {
        return new GraphChange() {
            @Override
            public void execute() {
                deleteAction(element);
            }

            @Override
            public void undo() {
                addAction(element);
            }
        };
    }

    public GraphChange clearAllElements(List<Node> allElements) {
        return new GraphChange() {
            @Override
            public void execute() {
                graph.getChildren().clear();
            }

            @Override
            public void undo() {
                graph.getChildren().addAll(allElements);
            }
        };
    }

    public GraphChange cutGraphElements(List<BaseVertex> buffer, List<Selectable> elements) {
        return new GraphChange() {
            @Override
            public void execute() {
                deleteSelection(buffer);
            }

            @Override
            public void undo() {
                addSelection(buffer);
            }
        };
    }

    public GraphChange pasteGraphElements(List<BaseVertex> buffer) {
        return new GraphChange() {
            @Override
            public void execute() {
                addSelection(buffer);
            }

            @Override
            public void undo() {
                deleteSelection(buffer);
            }
        };
    }

    private void addSelection(List<BaseVertex> buffer) {
        for (BaseVertex vertex : buffer) {
            for (Arc arc : vertex.getAdjacentArcs()) {
                addAction(arc);
            }
            addAction(vertex);
        }
    }


    private void addAction(Selectable element) {
        if (element instanceof BaseVertex vertex) {
            for (Arc arc : vertex.getInArcs()) {
                if (graph.getChildren().contains(arc.getSource()) && !graph.getChildren().contains(arc)) {
                    graph.getChildren().addAll(arc.asChild());
                    if (!arc.getSource().getOutArcs().contains(arc)) arc.getSource().getOutArcs().add(arc);
                }
            }
            for (Arc arc : vertex.getOutArcs()) {
                if (graph.getChildren().contains(arc.getTarget()) && !graph.getChildren().contains(arc)) {
                    graph.getChildren().addAll(arc.asChild());
                    if (!arc.getSource().getOutArcs().contains(arc)) arc.getTarget().getInArcs().add(arc);
                }
            }
            graph.getChildren().addAll(element.asChild());
        }
        if (element instanceof Arc arc && !graph.getChildren().contains(arc)) {
            if (!arc.getSource().getOutArcs().contains(arc)) arc.getSource().getOutArcs().add(arc);
            if (!arc.getTarget().getInArcs().contains(arc)) arc.getTarget().getInArcs().add(arc);
            graph.getChildren().addAll(element.asChild());
        }
    }

    private void deleteSelection(List<BaseVertex> buffer) {
        for (BaseVertex vertex : buffer) {
            for (Arc arc : vertex.getInArcs()) {
                if (!buffer.contains(arc.getSource())) {
                    arc.getSource().getOutArcs().remove(arc);
                }
                graph.getChildren().removeAll(arc.asChild());
            }
            for (Arc arc : vertex.getOutArcs()) {
                if (!buffer.contains(arc.getTarget())) {
                    arc.getTarget().getInArcs().remove(arc);
                }
                graph.getChildren().removeAll(arc.asChild());
            }
            graph.getChildren().removeAll(vertex.asChild());
        }
    }

    private void deleteAction(Selectable element) {
        if (element instanceof BaseVertex vertex) {
            for (Arc arc : vertex.getInArcs()) {
                if (graph.getChildren().contains(arc.getSource())) {
                    graph.getChildren().removeAll(arc.asChild());
                    arc.getSource().getOutArcs().remove(arc);
                }
            }
            for (Arc arc : vertex.getOutArcs()) {
                if (graph.getChildren().contains(arc.getTarget())) {
                    graph.getChildren().removeAll(arc.asChild());
                    arc.getTarget().getInArcs().remove(arc);
                }
            }
        }
        if (element instanceof Arc arc) {
            arc.getSource().getOutArcs().remove(arc);
            arc.getTarget().getInArcs().remove(arc);
        }
        graph.getChildren().removeAll(element.asChild());
    }

    public GraphChange moveGraphVertex(BaseVertex vertex, double curX, double curY, double prevX, double prevY) {
        return new GraphChange() {
            @Override
            public void execute() {
                vertex.setLayoutX(curX);
                vertex.setLayoutY(curY);
            }

            @Override
            public void undo() {
                vertex.setLayoutX(prevX);
                vertex.setLayoutY(prevY);
            }
        };
    }

    public GraphChange moveArcPoint(ArcPoint point, double curX, double curY, double prevX, double prevY) {
        return new GraphChange() {
            @Override
            public void execute() {
                point.setLayoutX(curX);
                point.setLayoutY(curY);
            }

            @Override
            public void undo() {
                point.setLayoutX(prevX);
                point.setLayoutY(prevY);
            }
        };
    }

    public GraphChange setNameProperty(BaseVertex vertex, String oldName, String newName) {
        return new GraphChange() {
            @Override
            public void execute() {
                vertex.setName(newName);
            }

            @Override
            public void undo() {
                vertex.setName(oldName);
            }
        };
    }

    public GraphChange addPlaceType(PetriType newType) {
        return new GraphChange() {
            @Override
            public void execute() {
                types.add(newType);
            }

            @Override
            public void undo() {
                types.remove(newType);
            }
        };
    }

    public GraphChange deletePlaceType(PetriType deletedType) {
        return new GraphChange() {
            @Override
            public void execute() {
                types.remove(deletedType);
            }

            @Override
            public void undo() {
                types.add(deletedType);
            }
        };
    }

    public GraphChange updatePlaceType(Place place, List<PetriType> allTypes, PetriType oldType, PetriType newType) {
        return new GraphChange() {
            @Override
            public void execute() {
                place.setType(newType);
            }

            @Override
            public void undo() {
                if (allTypes.contains(oldType)) place.setType(oldType);
            }
        };
    }

    public GraphChange updateTokens(Place place, int oldVal, int newVal) {
        return new GraphChange() {
            @Override
            public void execute() {
                place.setStartTokens(newVal);
            }

            @Override
            public void undo() {
                place.setStartTokens(oldVal);
            }
        };
    }

    public GraphChange setEndPlace(Place place, boolean oldVal, boolean newVal) {
        return new GraphChange() {
            @Override
            public void execute() {
                place.setEndPlace(newVal);
            }

            @Override
            public void undo() {
                place.setEndPlace(oldVal);
            }
        };
    }

    public GraphChange updateArcWeight(Arc arc, int oldWeight, int newWeight) {
        return new GraphChange() {
            @Override
            public void execute() {
                arc.setWeight(newWeight);
            }

            @Override
            public void undo() {
                arc.setWeight(oldWeight);
            }
        };
    }

    public GraphChange setVariable(Arc arc, boolean oldVal ,boolean newVal) {
        return new GraphChange() {
            @Override
            public void execute() {
                arc.setVariable(newVal);
            }

            @Override
            public void undo() {
                arc.setVariable(oldVal);
            }
        };
    }

    public void addGraphAction(GraphChange change) {
        if (change == null) return;
        List<GraphChange> changes = new ArrayList<GraphChange>();
        changes.add(change);
        undoQueue.add(changes);
    }

    public void executeAll(List<GraphChange> changes) {
        if (changes == null || changes.isEmpty()) return;
        for (GraphChange change : changes) {
            change.execute();
        }
        undoQueue.add(changes);
    }

    public void execute(GraphChange change) {
        if (change == null) return;
        change.execute();
        addGraphAction(change);
    }

    public void undo() {
        if (undoQueue.isEmpty()) return;
        List<GraphChange> changes = undoQueue.pollLast();
        redoQueue.add(changes);
        for (GraphChange change : changes) {
            change.undo();
        }
    }

    public void redo() {
        if (redoQueue.isEmpty()) return;
        List<GraphChange> changes = redoQueue.pollLast();
        undoQueue.add(changes);
        for (GraphChange change : changes) {
            change.execute();
        }
    }

    public void clear() {
        undoQueue.clear();
        redoQueue.clear();
    }

}
