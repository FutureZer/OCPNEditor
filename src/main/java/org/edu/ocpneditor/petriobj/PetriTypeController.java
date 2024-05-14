package org.edu.ocpneditor.petriobj;

import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class PetriTypeController implements Initializable {

    @FXML
    private Button DeleteObject;

    @FXML
    private Button addObject;

    @FXML
    private ColorPicker colorField;

    @FXML
    private Button exitButton;

    @FXML
    private TextField nameField;

    @FXML
    private TableColumn<PetriType, String> nameColumn;

    @FXML
    private TableColumn<PetriType, Color> colorColumn;

    @FXML
    private TableColumn<PetriType, String> idColumn;

    @FXML
    private TableView<PetriType> objectList;

    @FXML
    private Button submitButton;

    private List<PetriType> createdObjects;

    private boolean isObjectSelected = false;

    private PetriType selection;

    public void initCreatedObjects(List<PetriType> objects) {
        createdObjects = objects;
        objectList.getItems().addAll(createdObjects);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        colorColumn.setCellValueFactory(e -> new SimpleObjectProperty<Color>(e.getValue().getColor()));
        colorColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<PetriType, Color> call(TableColumn<PetriType, Color> petriObjectStringTableColumn) {
                return new TableCell<>() {
                    @Override
                    public void updateItem(Color item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!isEmpty()) {
                            this.setTextFill(item);
                            backgroundProperty().set(Background.fill(item));
                        }
                    }
                };
            }
        });
        initListeners();
    }

    private void initListeners() {
        objectList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal ,newVal) -> {
            if (newVal != null) {
                nameField.textProperty().set(newVal.getName());
                colorField.setValue(newVal.getColor());
            }
            nameField.setDisable(newVal == null);
            colorField.setDisable(newVal == null);
        });
        nameField.textProperty().addListener((obs, oldVal ,newVal) -> {
            objectList.getSelectionModel().getSelectedItem().setName(nameField.getText());
            objectList.refresh();
        });
        colorField.valueProperty().addListener((obs, oldVal ,newVal) -> {
            objectList.getSelectionModel().getSelectedItem().setColor(colorField.getValue());
            objectList.refresh();
        });
    }

    @FXML
    void addObjectClicked(MouseEvent event) {
        PetriType item = new PetriType();
        objectList.getItems().add(item);
        createdObjects.add(item);
    }

    @FXML
    void deleteObjectClicked(MouseEvent event) {
        PetriType selectedItem = objectList.getSelectionModel().getSelectedItem();
        if (selectedItem == PetriType.getDefaultType()) return;
        objectList.getItems().remove(selectedItem);
        createdObjects.remove(selectedItem);
        objectList.refresh();
    }

    @FXML
    public void selectButtonClicked(MouseEvent event) {
        selection = objectList.getSelectionModel().getSelectedItem();
        ((Node)event.getSource()).getScene().getWindow().hide();
    }

    @FXML
    public void cancelButtonClicked(MouseEvent event) {
        ((Node)event.getSource()).getScene().getWindow().hide();
    }

    public PetriType getSelection() {
        return selection;
    }
}
