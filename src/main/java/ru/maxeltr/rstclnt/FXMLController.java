package ru.maxeltr.rstclnt;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

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
    private TableColumn<FileModel, String> size;

    @FXML
    private TableColumn<FileModel, String> type;

    @FXML
    private AnchorPane textOrImagePane;

    private ObservableList<FileModel> items = FXCollections.observableArrayList();

    private File currentFolder;

    private TextArea logTextArea;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        name.setCellValueFactory(new PropertyValueFactory<>("Name"));
        date.setCellValueFactory(new PropertyValueFactory<>("Date"));
        size.setCellValueFactory(new PropertyValueFactory<>("size"));
        type.setCellValueFactory(new PropertyValueFactory<>("type"));

        logTextArea = new TextArea();
        logTextArea.setMaxHeight(Double.MAX_VALUE);
    }

    @FXML
    private void handleChooseFolder(ActionEvent event) {
        final DirectoryChooser chooser = new DirectoryChooser();
        Window stage = (Stage) root.getScene().getWindow();
        currentFolder = chooser.showDialog(stage);
        if (currentFolder != null) {
            fileTable.getItems().clear();
            logTextArea.setText("");
            File[] files = currentFolder.listFiles((File dir, String name1) -> name1.toLowerCase().endsWith(".log") || name1.toLowerCase().endsWith(".jpg"));
            for (File file : files) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                String extension = "";
                String fileName = file.getName();
                int i = fileName.lastIndexOf('.');
                if (i > 0) {
                    extension = fileName.substring(i + 1);
                }
                items.add(new FileModel(fileName, sdf.format(file.lastModified()), "" + file.length(), extension));
            }
            fileTable.setItems(items);
        } else {

        }
    }

    @FXML
    private void handleFileTableClicked(MouseEvent event) {
        if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
            FileModel fileModel = fileTable.getSelectionModel().getSelectedItem();
            if (fileModel != null) {
                if (!textOrImagePane.getChildren().contains(logTextArea)) {
                    textOrImagePane.getChildren().add(logTextArea);
                }
                logTextArea.setText(fileModel.getName());

            }
        }
    }

    @FXML
    private void handleButtonAction(ActionEvent event) {
        this.messsageNotImplemented();
    }

    @FXML
    private void handleExit(ActionEvent event) {
        System.exit(0);
    }

    @FXML
    private void handleAbout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("RestClient");
        alert.setHeaderText(null);
        alert.setContentText("This program is for viewing special log files which are located both remote and local. Project started 09.08.2019");
        alert.showAndWait();
    }

    private void messsageNotImplemented() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText("Not implemented!");

        alert.showAndWait();
    }
}
