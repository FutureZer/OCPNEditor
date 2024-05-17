package org.edu.ocpneditor.fileparser;

import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import org.edu.ocpneditor.panemodels.*;
import org.edu.ocpneditor.petriobj.PetriType;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCNetDeserializer {

    private final Color[] COLORS = {Color.GREEN, Color.BLUE, Color.MAGENTA, Color.ORANGE, Color.BROWN, Color.CYAN};
    private AnchorPane graph;

    private Map<String, BaseVertex> vertices = new HashMap<>();

    private List<Arc> arcs = new ArrayList<>();

    private List<PetriType> types = new ArrayList<>();

    private final List<Character> separators = List.of(new Character[]{' ', '\n', '\r', '\t', ';'});

    private final String separatorsRegex = "\\s|\n|\r|\t|;";

    public OCNetDeserializer(AnchorPane graph) {
        this.graph = graph;
    }

    public List<BaseVertex> getVertices() {
        return List.copyOf(vertices.values());
    }

    public List<Arc> getArcs() {
        return arcs;
    }

    public List<PetriType> getTypes() {
        return types;
    }

    public void deserializeModel(File file) throws IOException {
        StringBuilder lines = new StringBuilder();
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.append(line + "\n");
                }
            }
        } else {
            throw new OCNetDeserializationException("User didn't select model file");
        }
        StringBuilder body = new StringBuilder().append(findModelSection(lines));
        initVertices(body, OCDot.TRANSITIONS);
        initVertices(body, OCDot.PLACES);
        initArcs(body.toString());
    }

    public void deserializeConfiguration(File file) throws IOException {
        Yaml yaml = new Yaml();
        if (file == null) {
            throw new OCNetDeserializationException("User didn't select configuration file");
        }
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            Map<String, Object> objs = yaml.load(inputStream);
            Configuration configuration = new Configuration(objs, separatorsRegex);
            initConfiguration(configuration);
        }
    }

    private void initConfiguration(Configuration configuration) {
        for (String id : configuration.getInputPlaces()) {
            if (vertices.get(id) instanceof Place place) {
                place.setStartTokens(1);
            }
        }
        for (String id : configuration.getOutputPlaces()) {
            if (vertices.get(id) instanceof Place place) {
                place.setEndPlace(true);
            }
        }
        for (String id : configuration.getInitialMarking().keySet()) {
            if (vertices.get(id) instanceof Place place) {
                place.setStartTokens(configuration.getInitialMarking().get(id));
            }
        }
        int index = 0;
        for (String id : configuration.getPlaceTyping().keySet()) {
            PetriType type = new PetriType(id, index < COLORS.length ? COLORS[index++] : Color.BLACK);
            for (String vertexId : configuration.getPlaceTyping().get(id)) {
                if (vertices.get(vertexId) instanceof Place place) {
                    place.setType(type);
                }
            }
            types.add(type);
        }
    }

    private String findModelSection(StringBuilder lines) throws OCNetDeserializationException {
        String regex = OCDot.START + "\\s*\\" + OCDot.START_SEC;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(lines);
        if (!matcher.find()) throw new OCNetDeserializationException("There is no OCNet body in the file");

        int depth = 0;
        int endFile = -1;
        for (int i = matcher.end() - 1; i < lines.length(); ++i) {
            if (lines.charAt(i) == OCDot.START_SEC.charAt(0)) depth++;
            if (lines.charAt(i) == OCDot.END_SEC.charAt(0)) depth--;
            if (depth == 0) {
                endFile = i;
                break;
            }
        }
        if (endFile == -1) throw new OCNetDeserializationException("There is no end bracket (mark) for OCNet");

        return lines.substring(matcher.end(), endFile);
    }

    private void initVertices(StringBuilder lines, String section) throws OCNetDeserializationException {
        String regex = section + "\\s*\\" + OCDot.START_SEC;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(lines);
        if (!matcher.find()) return;
        int startSec = matcher.start();
        int endSec = -1;
        StringBuilder currentId = new StringBuilder();
        boolean isId = false;
        for (int i = matcher.end(); i < lines.length(); ++i) {
            char current = lines.charAt(i);
            if (current == OCDot.END_SEC.charAt(0)) {
                endSec = i;
                break;
            }
            if (separators.contains(lines.charAt(i))) {
                if (isId) {
                    isId = false;
                    if (vertices.containsKey(currentId.toString())) {
                        throw new OCNetDeserializationException("Fined two identical identifiers: " + currentId);
                    }
                    if (Objects.equals(section, OCDot.TRANSITIONS)) {
                        vertices.put(currentId.toString(), new Transition(0, 0));
                    } else if (Objects.equals(section, OCDot.PLACES)) {
                        vertices.put(currentId.toString(), new Place(0 ,0));
                    }
                }
            } else if (Character.isLetter(current) || Character.isDigit(current)) {
                if (!isId) currentId = new StringBuilder();
                currentId.append(lines.charAt(i));
                isId = true;
            } else if (current == '/') {
                i = skipCommentSections(lines, i);
            } else if (current == '[') {
                int end = lines.indexOf("]", i);
                initVertexProperties(vertices.get(currentId.toString()), lines.substring(i, end));
                i = lines.indexOf("]", i);
            } else {
                throw new OCNetDeserializationException("Invalid character " + lines.charAt(i) + " was found");
            }
        }
        if (endSec == -1)
            throw new OCNetDeserializationException("No end section sign for " + section + " section");
        lines.replace(startSec, endSec + 1, "");
    }

    private int skipCommentSections(StringBuilder lines, int index) throws OCNetDeserializationException {
        int end = -1;
        if (lines.charAt(index + 1) == '/') {
            end = lines.indexOf("\n", index);
        } else if (lines.charAt(index + 1) == '*') {
            end = lines.indexOf("*/", index);
            end++;
        }
        if (end == -1 || end > lines.length()) {
            throw new OCNetDeserializationException("Incorrect or invalid comment section");
        }
        return end;
    }

    private void initVertexProperties(BaseVertex vertex, String props) {
        if (vertex == null) return;
        Pattern pattern = Pattern.compile("label=\".*\"");
        Matcher matcher = pattern.matcher(props);
        if (matcher.find()) {
            int labelIndex = props.indexOf("label=\"");
            int from = props.indexOf("\"", labelIndex);
            int to = props.indexOf("\"", from + 1);
            String label = props.substring(from + 1, to);
            vertex.setName(label);
        }
        if (!(vertex instanceof Place)) return;
        Place place = (Place) vertex;
        pattern = Pattern.compile("color=\".*\"");
        matcher = pattern.matcher(props);
        if (matcher.find()) {
            int labelIndex = props.indexOf("color=\"");
            int from = props.indexOf("\"", labelIndex);
            int to = props.indexOf("\"", from + 1);
            String colorHex = props.substring(from, to);
            try {
                Color color = new Color(Integer.valueOf(colorHex.substring(1, 3), 16),
                        Integer.valueOf(colorHex.substring(3, 5), 16),
                        Integer.valueOf(colorHex.substring(5, 7), 16), 0);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void initArcs(String lines) {
        String[] elements = Arrays.stream(lines.split(separatorsRegex)).filter(e -> !e.trim().isEmpty()).toArray(String[]::new);
        String fromId = null;
        String arcStr = null;
        for (String currentId : elements) {
            if (currentId.equals(OCDot.ARC) || currentId.equals(OCDot.VAR_ARC)) {
                arcStr = currentId;
            } else {
                if (fromId != null && arcStr != null) {
                    BaseVertex from = vertices.get(fromId);
                    BaseVertex to = vertices.get(currentId);
                    Arc arc = new Arc(from, to);
                    if (arcStr.equals(OCDot.VAR_ARC)) {
                        arc.setVariable(true);
                    }
                    arcs.add(arc);
                    arcStr = null;
                }
                fromId = currentId;
            }
        }
    }

}
