package org.edu.ocpneditor.algos;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import org.edu.ocpneditor.panemodels.Arc;
import org.edu.ocpneditor.panemodels.Place;
import org.edu.ocpneditor.panemodels.Transition;

import java.util.ArrayList;
import java.util.List;

public class PetriValidation {

    private final List<Transition> transitions = new ArrayList<>();


    public PetriValidation(AnchorPane graph) {
        for (Node node : graph.getChildren()) {
            if (node instanceof Transition vertex) this.transitions.add(vertex);
        }
    }

    public void validation() {
        if (transitions.isEmpty()) noMistakes();
        boolean result = true;
        for (Transition tr : transitions) {
            result = isTransitionCorrect(tr);
            if (!result) break;
        }
        if (result) {
            noMistakes();
        }
    }

    private boolean isTransitionCorrect(Transition transition) {
        int inVariable = 0;
        int outVariable = 0;
        Arc outVarArc = null;
        Arc inVarArc = null;
        for (Arc arc : transition.getInArcs()) {
            if (arc.isVariable()) {
                inVariable++;
                inVarArc = arc;
            }
        }
        for (Arc arc : transition.getOutArcs()) {
            if (arc.isVariable()) {
                outVariable++;
                outVarArc = arc;
            }
        }
        if (inVariable > 1) {
            toManyInVariable(transition);
            return false;
        }
        if (outVariable > 1) {
            toManyOutVariable(transition);
            return false;
        }
        if (inVariable == 1 && outVariable == 0) {
            noIncomingVariable(transition);
            return false;
        }
        if (outVariable == 1 && inVariable == 0) {
            noOutgoingVariable(transition);
            return false;
        }
        if (inVariable == 1 && outVariable == 1) {
            if (!(((Place) outVarArc.getTarget()).getType().equals(((Place) inVarArc.getSource()).getType()))) {
                typeMismatch(transition);
                return false;
            }
        }
        return true;
    }

    private void setAlertHeader(Alert alert) {
        alert.setTitle("Validation");
        alert.setHeaderText("Validation result");
        alert.setResizable(true);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    }

    private void toManyInVariable(Transition transition) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        setAlertHeader(alert);
        alert.setContentText("There is two variable arcs leading to the transition " + transition.info());
        alert.showAndWait();
    }

    private void toManyOutVariable(Transition transition) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        setAlertHeader(alert);
        alert.setContentText("There is two or more variable arcs outgoing from the transition " + transition.info());
        alert.showAndWait();
    }

    private void noIncomingVariable(Transition transition) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        setAlertHeader(alert);
        alert.setContentText("There is incoming variable arc, but no outgoing variable arc from the transition " + transition.info());
        alert.showAndWait();
    }

    private void noOutgoingVariable(Transition transition) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        setAlertHeader(alert);
        alert.setContentText("There is outgoing variable arc, but no incoming variable arc to the transition " + transition.info());
        alert.showAndWait();
    }

    private void typeMismatch(Transition transition) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        setAlertHeader(alert);
        alert.setContentText("The type of outgoing and incoming variable arcs do not match for the transition " + transition.info());
        alert.showAndWait();
    }

    private void noMistakes() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        setAlertHeader(alert);
        alert.setContentText("There is no errors in the net");
        alert.showAndWait();
    }

}
