package org.edu.ocpneditor.algos;

import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import org.edu.ocpneditor.panemodels.Arc;
import org.edu.ocpneditor.panemodels.BaseVertex;
import org.edu.ocpneditor.panemodels.Place;
import org.edu.ocpneditor.panemodels.Transition;
import org.edu.ocpneditor.utils.GraphActionManager;
import org.edu.ocpneditor.utils.GraphChange;

import java.util.*;

public class GraphLayout {

    List<BaseVertex> root = new ArrayList<>();

    Set<BaseVertex> visited = new HashSet<>();

    Queue<BaseVertex> queue = new ArrayDeque<>();

    Queue<Integer> queueLevel = new ArrayDeque<>();

    HashMap<Integer, List<BaseVertex>> levels = new HashMap<>();

    double paddingX = 40;
    double paddingY = 40;

    public GraphLayout(AnchorPane graph) {
        List<BaseVertex> allNodes = new ArrayList<>();
        for (Node node : graph.getChildren()) {
            if (node instanceof BaseVertex) allNodes.add((BaseVertex) node);
            if (node instanceof Arc arc) arc.clearPoints();
        }
        initRoots(allNodes);
        for (BaseVertex node: root) {
            queue.add(node);
            queueLevel.add(0);
        }
        while (!queue.isEmpty() && !queueLevel.isEmpty()) {
            BaseVertex vertex = queue.poll();
            buildTree(vertex, queueLevel.poll());
            allNodes.remove(vertex);
            if (queue.isEmpty() && !allNodes.isEmpty()) {
                BaseVertex disjoint = allNodes.get(0);
                queue.add(disjoint);
                if (disjoint instanceof Place) queueLevel.add(0);
                if (disjoint instanceof Transition) queueLevel.add(1);
            }
        }
    }

    private void initRoots(List<BaseVertex> allNodes) {

        List<BaseVertex> potentialRoot = new ArrayList<>();
        Place pStart = null;
        Transition tStart = null;

        // Firstly we want to find positions, that do not have income arcs
        // If there is no such vertices, then select place randomly (or transition if there is no places)

        for (BaseVertex node : allNodes) {
            if (node instanceof Place place) {
                if (place.getInArcs().isEmpty()) potentialRoot.add(node);
                if (pStart == null || pStart.getGraphId().compareTo(place.getGraphId()) > 0) {
                    pStart = place;
                }
            }
            if (node instanceof Transition transition) {
                if (tStart == null || tStart.getGraphId().compareTo(transition.getGraphId()) > 0) {
                    tStart = transition;
                }
            }
        }


        if (!potentialRoot.isEmpty()) {
            root = potentialRoot;
        } else {
            root.add(pStart != null ? pStart : tStart);
        }
    }

    private void buildTree(BaseVertex node, int level) {
        levels.putIfAbsent(level, new ArrayList<>());
        if (!visited.contains(node)) {
            levels.get(level).add(node);
            visited.add(node);
            for (Arc inArcs : node.getInArcs()) {
                queue.add(inArcs.getSource());
                queueLevel.add(level + 1);
            }
            for (Arc inArcs : node.getOutArcs()) {
                queue.add(inArcs.getTarget());
                queueLevel.add(level + 1);
            }
        }
    }

    public List<GraphChange> arrange(GraphActionManager actions) {
        List<GraphChange> changes = new ArrayList<>();
        int maxInRow = 0;
        for (Integer level : levels.keySet()) {
            maxInRow = Math.max(maxInRow, levels.get(level).size());
        }

        double maxHeight = maxInRow * paddingY * 2;
        double x = paddingX;
        double y = 0;

        for (Integer level : levels.keySet()) {

            double currentPadding = maxHeight / (levels.get(level).size() + 1);

            for (BaseVertex vertex : levels.get(level)) {
                y += currentPadding;
                changes.add(actions.moveGraphVertex(vertex, x, y, vertex.getLayoutX(), vertex.getLayoutY()));
            }

            y = 0;
            x += paddingX * 2;
        }

        return changes;
    }
}
