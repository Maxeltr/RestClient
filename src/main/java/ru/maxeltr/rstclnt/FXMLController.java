package ru.maxeltr.rstclnt;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import java.io.File;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.control.ListView;

public class FXMLController implements Initializable {

    @FXML
    private Parent root;

    @FXML
    private ListView fileList;

    @FXML
    private Label label;

    private ObservableList<String> items = FXCollections.observableArrayList();

    @FXML
    private void handleButtonAction(ActionEvent event) {
        final DirectoryChooser chooser = new DirectoryChooser();
        Window stage = (Stage) root.getScene().getWindow();
        File folder = chooser.showDialog(stage);
        if (folder != null) {
            items.add("First task");
            items.add("Second task");
        } else {
            label.setText(null);
        }

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        fileList.setItems(items);
    }
}
