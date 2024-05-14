package org.edu.ocpneditor.panemodels;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.text.Font;
import org.edu.ocpneditor.petriobj.PetriType;

public class Place extends BaseVertex {

    private static final String STYLE = "place";

    SimpleObjectProperty<PetriType> type =  new SimpleObjectProperty<>();

    private SimpleIntegerProperty startTokens = new SimpleIntegerProperty();
    private SimpleBooleanProperty isEndPlace = new SimpleBooleanProperty();


    public Place(double x, double y) {
        super(x, y);
        getStyleClass().add(STYLE);
        setType(PetriType.getDefaultType());
        startTokens.set(0);
        isEndPlace.set(false);

        initListeners();
        graphId = "pl" + count++;
    }

    private Place(Place place) {
        super(place);
        getStyleClass().add(STYLE);
        type.set(place.getType());
        startTokens.set(place.getStartTokens());
        isEndPlace.set(place.isEndPlace());

        initListeners();
        graphId = "pl" + count++;
    }

    private void initListeners() {
        type.addListener((obsVal, oldVal, newVal) -> {
            if (newVal != null) {
                setStyle(newVal.getBorderStyle());
                for (Arc arc : getAdjacentArcs()) {
                    arc.setTypeColor(newVal.getColor());
                }
            }
        });
        startTokens.addListener(((observableValue, oldVal, newVal) -> {
            if (newVal.intValue() > 0) {
                setText(newVal.toString());
            } else {
                setText("");
            }
        }));
        isEndPlace.addListener((observable, oldVal, newVal) -> {
            if (newVal) {
                setText("E");
            } else {
                setText("");
                setFont(Font.getDefault());
            }
        });
    }

    public PetriType getType() {
        return type.get();
    }

    public void setType(PetriType type) {
        setStyle(type.getBorderStyle());
        this.type.set(type);
    }

    public int getStartTokens() {
        return startTokens.getValue();
    }

    public void setStartTokens(int startTokens) {
        this.startTokens.set(startTokens);
    }

    public boolean isEndPlace() {
        return isEndPlace.get();
    }

    public void setEndPlace(boolean endPlace) {
        isEndPlace.set(endPlace);
    }

    @Override
    public BaseVertex copy() {
        return new Place(this);
    }
}
