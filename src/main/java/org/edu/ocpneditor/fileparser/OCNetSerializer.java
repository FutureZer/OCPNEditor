package org.edu.ocpneditor.fileparser;

import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import org.edu.ocpneditor.panemodels.Arc;
import org.edu.ocpneditor.panemodels.BaseVertex;
import org.edu.ocpneditor.panemodels.Place;
import org.edu.ocpneditor.panemodels.Transition;
import org.edu.ocpneditor.petriobj.PetriType;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

public class OCNetSerializer {

    private final String FILE_START = OCDot.START + "  " + OCDot.START_SEC + System.lineSeparator();
    private final String TRANSITION_SEC = OCDot.TRANSITIONS + " " + OCDot.START_SEC  + System.lineSeparator();
    private final String PLACE_SEC = OCDot.PLACES + " " + OCDot.START_SEC + System.lineSeparator();
    private final String ENT_SEC = OCDot.END_SEC + System.lineSeparator();
    private final String FILE_TAB = "  ";
    private final String ARC = " " + OCDot.ARC + " ";
    private final String VAR_ARC = " " + OCDot.VAR_ARC + " ";

    private AnchorPane graph;
    private List<PetriType> types;

    public OCNetSerializer(AnchorPane graph, List<PetriType> objects) {
        this.graph = graph;
        this.types = objects;
    }

    public void serializeModel(File file) throws IOException {
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(makeFileContent());
            }
        }
    }

    public void serializeConfiguration(File file) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(4);
        Yaml yaml = new Yaml(options);
        Configuration configuration = new Configuration(graph, types);
        String fileContent = yaml.dump(configuration);
        fileContent = fileContent.replace(fileContent.split("\n")[0], "");
        fileContent = fileContent.replace("'", "").replaceAll("\\s*-\\s", " ");

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(fileContent);
            }
        }
    }

    private String tabs(int required) {
        return new String(new char[required]).replace("\0", FILE_TAB);
    }

    private String makeFileContent() {
        StringBuilder fileContent = new StringBuilder();

        int requiredTabs = 1;

        fileContent.append(FILE_START);

        // Place section
        fileContent.append(tabs(requiredTabs++)).append(PLACE_SEC);
        for (Node node : graph.getChildren()) {
            if (!(node instanceof Place place)) continue;
            fileContent.append(tabs(requiredTabs))
                    .append(place.getGraphId()).append(" ")
                    .append(getProperties(place))
                    .append(System.lineSeparator());
        }
        fileContent.append(tabs(--requiredTabs)).append(ENT_SEC);

        // Transition section
        fileContent.append(tabs(requiredTabs++)).append(TRANSITION_SEC);
        for (Node node : graph.getChildren()) {
            if (!(node instanceof Transition transition)) continue;
            fileContent.append(tabs(requiredTabs))
                    .append(transition.getGraphId()).append(" ")
                    .append(getProperties(transition))
                    .append(System.lineSeparator());
        }
        fileContent.append(tabs(--requiredTabs)).append(ENT_SEC);

        // Arcs section
        List<String> chains = getNodesChain();
        for (String chain: chains) {
            fileContent.append(tabs(requiredTabs)).append(chain).append(System.lineSeparator());
        }

        fileContent.append(ENT_SEC);

        return fileContent.toString();
    }

    private String getProperties(BaseVertex vertex) {
        StringBuilder props = new StringBuilder();
        props.append("[");
        String label = vertex.getName();
        if (label.isEmpty()) label = vertex.getGraphId();
        props.append("label=\"").append(label).append("\"");
        if (vertex instanceof Place) props.append(", ").append("color=\"").append(((Place)vertex).getType().getHex()).append("\"");
        props.append("]");
        return props.toString();
    }

    private List<String> getNodesChain() {
        List<String> chainsStr = new ArrayList<>();
        Map<BaseVertex, Boolean> isVisited = new HashMap<>();
        List<Place> initials = new ArrayList<>();
        for (Node node : graph.getChildren()) {
            if (node instanceof BaseVertex) isVisited.put((BaseVertex) node, false);
            if (node instanceof Place && ((Place)node).getStartTokens() > 0) initials.add((Place)node);
        }

        BaseVertex start = getUnvisited(isVisited, initials);

        while (start != null) {
            getAllChains(chainsStr, new StringBuilder(), isVisited, initials, start);
            start = getUnvisited(isVisited, initials);
        }

        return chainsStr;
    }

    private BaseVertex getUnvisited(Map<BaseVertex, Boolean> visitations, List<Place> initials) {
        if (!initials.isEmpty()) return initials.get(0);
        else {
            for (BaseVertex node : visitations.keySet()) {
                if (!visitations.get(node)) return node;
            }
            return null;
        }
    }

    private void getAllChains(List<String> chains, StringBuilder chain, Map<BaseVertex, Boolean> visitations, List<Place> initials, BaseVertex current) {
        if (visitations.get(current)) {
            chain.append(current.getGraphId());
            chains.add(chain.toString());
            return;
        }
        if (current instanceof Place) initials.remove(current);
        visitations.put(current, true);
        boolean isFirst = true;
        if (current.getOutArcs().isEmpty()) {
            if (chain.isEmpty()) return;
            chain.append(current.getGraphId());
            chains.add(chain.toString());
            return;
        }
        for (Arc arc : current.getOutArcs()) {
            if (isFirst) {
                chain.append(current.getGraphId()).append(arc.isVariable() ? VAR_ARC : ARC);
                getAllChains(chains, chain, visitations, initials, arc.getTarget());
                isFirst = false;
            } else {
                StringBuilder newChain = new StringBuilder();
                newChain.append(current.getGraphId()).append(arc.isVariable() ? VAR_ARC : ARC);
                getAllChains(chains, newChain, visitations, initials, arc.getTarget());
            }
        }
    }
}
