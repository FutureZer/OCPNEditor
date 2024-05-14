package org.edu.ocpneditor.panemodels;

import javafx.scene.Node;

import java.util.List;

public interface Selectable {
    public void setSelectedStyle();

    public void setUnselectedStyle();

    public List<Node> asChild();
}
