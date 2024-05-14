module org.edu.ocpneditor {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.yaml.snakeyaml;


    opens org.edu.ocpneditor to javafx.fxml;
    opens org.edu.ocpneditor.petriobj to javafx.fxml, javafx.base;
    exports org.edu.ocpneditor;
    exports org.edu.ocpneditor.view;
    exports org.edu.ocpneditor.fileparser;
    exports org.edu.ocpneditor.panemodels;
    opens org.edu.ocpneditor.view to javafx.fxml;
}