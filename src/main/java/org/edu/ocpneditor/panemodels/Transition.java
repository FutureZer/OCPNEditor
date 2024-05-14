package org.edu.ocpneditor.panemodels;

public class Transition extends BaseVertex {

    public static final String STYLE = "transition";

    public Transition(double x, double y) {
        super(x, y);
        getStyleClass().add(STYLE);
        graphId = "tr" + count++;
    }

    private Transition(Transition transition) {
        super(transition);

        getStyleClass().add(STYLE);
        graphId = "tr" + count++;
    }

    public String info() {
        StringBuilder builder = new StringBuilder();
        builder.append("ID: ").append(getGraphId());
        if (nameProperty().get().isEmpty()) return builder.toString();
        builder.append(", Name: ").append(getName());
        return builder.toString();
    }

    @Override
    public BaseVertex copy() {
        return new Transition(this);
    }
}
