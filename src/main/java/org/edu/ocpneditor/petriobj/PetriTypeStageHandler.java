package org.edu.ocpneditor.petriobj;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.edu.ocpneditor.panemodels.Arc;
import org.edu.ocpneditor.panemodels.Place;

import java.io.IOException;
import java.util.List;

public class PetriTypeStageHandler {

    private AnchorPane graph;
    private List<PetriType> petriTypes;

    public PetriTypeStageHandler(AnchorPane graph, List<PetriType> objects) {
        petriTypes = objects;
        this.graph = graph;
    }

    public PetriType selectAndUpdatePlaceType(Place selected) throws IOException {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("petriobjects-view.fxml"));

        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);

        PetriTypeController controller = loader.getController();
        controller.initCreatedObjects(petriTypes);

        stage.showAndWait();

        for (Node node : graph.getChildren()) {
            if (node instanceof Place place) {
                if (!petriTypes.contains(place.getType())) {
                    place.setType(PetriType.getDefaultType());
                }
                place.setStyle(place.getType().getBorderStyle());
                for (Arc arc : place.getAdjacentArcs()) {
                    arc.setTypeColor(place.getType().getColor());
                }
            }
        }

        PetriType object = controller.getSelection() != null ? controller.getSelection() :
                (selected != null ? selected.getType() : PetriType.getDefaultType());

        return object;
    }
}
