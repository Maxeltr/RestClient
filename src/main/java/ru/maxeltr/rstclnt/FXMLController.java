package ru.maxeltr.rstclnt;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.text.SimpleDateFormat;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.TableColumn;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class FXMLController implements Initializable {

    @FXML
    private Parent root;

    @FXML
    private TableView<FileModel> fileTable;

    @FXML
    private TableColumn<FileModel, Integer> name;

    @FXML
    private TableColumn<FileModel, String> date;

    @FXML
    private TableColumn<FileModel, String> type;

    private ObservableList<FileModel> items = FXCollections.observableArrayList();

    @FXML
    private void handleChooseFolder(ActionEvent event) {
        final DirectoryChooser chooser = new DirectoryChooser();
        Window stage = (Stage) root.getScene().getWindow();
        File folder = chooser.showDialog(stage);
        if (folder != null) {
            File[] files  = folder.listFiles();
            for (File file : files) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                items.add(new FileModel(file.getName(), sdf.format(file.lastModified()), "" + file.length()));
            }

            fileTable.setItems(items);
        } else {

        }
    }

    @FXML
    private void handleButtonAction(ActionEvent event) {
        final DirectoryChooser chooser = new DirectoryChooser();
        Window stage = (Stage) root.getScene().getWindow();
        File folder = chooser.showDialog(stage);
        if (folder != null) {

        } else {

        }

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        name.setCellValueFactory(new PropertyValueFactory<>("Name"));
        date.setCellValueFactory(new PropertyValueFactory<>("Date"));
        type.setCellValueFactory(new PropertyValueFactory<>("Type"));
    }
}
