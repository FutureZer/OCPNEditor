package org.edu.ocpneditor.petriobj;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

public class PetriType {

    private static PetriType emptyType = new PetriType("Nothing", Color.BLACK, "0");

    private static PetriType defaultType = emptyType;

    // Why buttons do not have stroke property???
    private String STYLE_START = "-fx-border-color: ";
    private String STYLE_END = ";";
    private String borderStyle;
    private String name;
    private SimpleObjectProperty<Color> color = new SimpleObjectProperty<>();
    private static int count = 1;
    private String id;

    private void initColor(Color color) {
        this.color.addListener((obsVal, oldVal, newVal) -> {
            if (newVal != null) {
                this.borderStyle = STYLE_START + getHex(newVal) + STYLE_END;
            }
        });
        this.borderStyle = STYLE_START + getHex(color) + STYLE_END;
        this.color.set(color);
    }

    private PetriType(String name, Color color, String id) {
        this.name = name;
        initColor(color);
        this.id = "pt" + id;
    }

    public PetriType(String name, Color color) {
        this.name = name;
        initColor(color);
        this.id = "pt" + count++;
    }

    public PetriType() {
        this.name = "";
        initColor(Color.BLACK);
        id = "pt" + count++;
    }

    private String format(double val) {
        String in = Integer.toHexString((int) Math.round(val * 255));
        return in.length() == 1 ? "0" + in : in;
    }

    public String getHex(Color value) {
        return "#" + (format(value.getRed()) + format(value.getGreen()) +
                format(value.getBlue()) + format(value.getOpacity()))
                .toUpperCase();
    }

    public String getHex() {
        return "#" + (format(color.get().getRed()) + format(color.get().getGreen()) +
                format(color.get().getBlue()) + format(color.get().getOpacity()))
                .toUpperCase();
    }

    public String getBorderStyle() {
        return borderStyle;
    }

    public void setBorderStyle(String typeColor) {
        this.borderStyle = typeColor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Color getColor() {
        return color.get();
    }

    public SimpleObjectProperty<Color> colorProperty() {
        return color;
    }

    public void setColor(Color color) {
        this.color.set(color);
    }

    public String getId() {
        return id;
    }

    public static PetriType getDefaultType() {
        return defaultType;
    }

    public static void setDefaultType(PetriType defaultType) {
        PetriType.defaultType = defaultType;
    }

    public static void setEmptyDefault() {
        defaultType = emptyType;
    }
}
