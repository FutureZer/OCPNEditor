package org.edu.ocpneditor.fileparser;

import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import org.edu.ocpneditor.panemodels.Place;
import org.edu.ocpneditor.petriobj.PetriType;

import java.util.*;


public class Configuration {

    private List<String> inputPlaces = new ArrayList<>();
    private List<String> outputPlaces = new ArrayList<>();
    private Map<String, Integer> initialMarking = new HashMap<>();
    private Map<String, List<String>> placeTyping = new HashMap<>();

    // From initial objects
    Configuration(AnchorPane graph, List<PetriType> object) {
        for (PetriType type : object) {
            placeTyping.put(type.getId(), new ArrayList<>());
        }
        for (Node node : graph.getChildren()) {
            if (!(node instanceof Place place)) continue;
            if (place.getStartTokens() > 0) {
                inputPlaces.add(place.getGraphId());
                initialMarking.put(place.getGraphId(), place.getStartTokens());
            }
            if (place.isEndPlace()) outputPlaces.add(place.getGraphId());
            placeTyping.get(place.getType().getId()).add(place.getGraphId());
        }
    }

    Configuration(Map<String, Object> info, String separators) {
        inputPlaces = getListValues(info.get("inputPlaces").toString(), separators);
        outputPlaces = getListValues(info.get("outputPlaces").toString(), separators);
        if (info.get("initialMarking") instanceof Map<?, ?> marking) {
            for (Object key : marking.keySet()) {
                if (key instanceof String && marking.get(key) instanceof Integer) {
                    initialMarking.put((String) key, (int) marking.get(key));
                }
            }
        }
        if (info.get("placeTyping") instanceof Map<?, ?> typing) {
            for (Object key : typing.keySet()) {
                if (key instanceof String && typing.get(key) instanceof String) {
                    placeTyping.put((String) key, getListValues((String) typing.get(key), separators));
                }
            }
        }
    }

    private List<String> getListValues(String values, String separators) {
        return List.of(Arrays.stream(values.split(separators)).filter(e -> !e.trim().isEmpty()).toArray(String[]::new));
    }

    public List<String> getInputPlaces() {
        return inputPlaces;
    }

    public void setInputPlaces(List<String> inputPlaces) {
        this.inputPlaces = inputPlaces;
    }

    public List<String> getOutputPlaces() {
        return outputPlaces;
    }

    public void setOutputPlaces(List<String> outputPlaces) {
        this.outputPlaces = outputPlaces;
    }

    public Map<String, Integer> getInitialMarking() {
        return initialMarking;
    }

    public void setInitialMarking(Map<String, Integer> initialMarking) {
        this.initialMarking = initialMarking;
    }

    public Map<String, List<String>> getPlaceTyping() {
        return placeTyping;
    }

    public void setPlaceTyping(Map<String, List<String>> placeTyping) {
        this.placeTyping = placeTyping;
    }
}
