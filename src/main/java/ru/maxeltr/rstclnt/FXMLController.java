package ru.maxeltr.rstclnt;

import javafx.geometry.Rectangle2D;
import java.io.BufferedReader;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;

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
    private StackPane textOrImagePane;

    private final ObservableList<FileModel> items = FXCollections.observableArrayList();

    private File currentFolder;

    private TextArea logTextArea;

    private ImageView logImageView;

    private ScrollPane logScrollPane;

    private double currentScale = 1;
    private final double SCALE_MIN = 0.1;
    private final double SCALE_MAX = 5;
    private double x;
    private double y;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        name.setCellValueFactory(new PropertyValueFactory<>("Name"));
        date.setCellValueFactory(new PropertyValueFactory<>("Date"));
        size.setCellValueFactory(new PropertyValueFactory<>("size"));
        type.setCellValueFactory(new PropertyValueFactory<>("type"));

        logTextArea = new TextArea();
        logTextArea.setMaxHeight(Double.MAX_VALUE);
        logTextArea.setEditable(false);

        logImageView = new ImageView();
        logImageView.setPreserveRatio(true);

        logScrollPane = new ScrollPane();
        logScrollPane.setPannable(true);
        logScrollPane.setContent(new Group(logImageView));

        logImageView.setOnScroll((ScrollEvent event) -> {
            double delta = event.getDeltaY();
            double scale = Math.pow(1.01, delta);

            double curScale = scale * currentScale;
            if (curScale >= SCALE_MIN && curScale <= SCALE_MAX) {
                logImageView.setScaleX(logImageView.getScaleX() * scale);
                logImageView.setScaleY(logImageView.getScaleY() * scale);
                currentScale = curScale;
            }
            event.consume();
        });
    }

    @FXML
    private void handleChooseFolder(ActionEvent event) throws IOException {
        final DirectoryChooser chooser = new DirectoryChooser();
        Window stage = (Stage) root.getScene().getWindow();
        currentFolder = chooser.showDialog(stage);
        if (currentFolder != null) {
            fileTable.getItems().clear();
            logTextArea.setText("");
            File[] files = currentFolder.listFiles((File dir, String name1) -> name1.toLowerCase().endsWith(".log") || name1.toLowerCase().endsWith(".jpg"));
            for (File file : files) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

                String fileType;
                String fileName = file.getName();
                fileType = Files.probeContentType(file.toPath());
                if (fileType == null) {
                    String extension = "";
                    int i = fileName.lastIndexOf('.');
                    if (i > 0) {
                        extension = fileName.substring(i + 1);
                    }
                    if ("log".equals(extension.toLowerCase())) {
                        fileType = "text/plain";
                    } else {
                        System.err.format("'%s' has an" + " unknown filetype.%n", file.toPath());
                        continue;
                    }
                } else {
                    if (!fileType.equals("image/jpeg") && !fileType.equals("text/plain")) {
                        System.err.format("'%s' has an" + " unsupported filetype.%n", file.toPath());
                        continue;
                    }
                }

                items.add(new FileModel(fileName, sdf.format(file.lastModified()), "" + file.length(), fileType));
            }
            fileTable.setItems(items);
        } else {

        }
    }

    @FXML
    private void handleFileTableClicked(MouseEvent event) throws FileNotFoundException, IOException {
        if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
            FileModel fileModel = fileTable.getSelectionModel().getSelectedItem();
            if (fileModel != null) {
                switch (fileModel.getType()) {
                    case ("text/plain"):
                        if (textOrImagePane.getChildren().contains(logImageView)) {
                            textOrImagePane.getChildren().remove(logImageView);
                        }
                        if (!textOrImagePane.getChildren().contains(logTextArea)) {
                            textOrImagePane.getChildren().add(logTextArea);
                        }
                        try (BufferedReader br = new BufferedReader(new FileReader(this.currentFolder + "\\" + fileModel.getName()))) {
                            StringBuilder sb = new StringBuilder();
                            String line = br.readLine();
                            while (line != null) {
                                sb.append(line);
                                sb.append(System.lineSeparator());
                                line = br.readLine();
                            }
                            String everything = sb.toString();
                            logTextArea.setText(everything);
                        }
                        break;
                    case ("image/jpeg"):
                        if (textOrImagePane.getChildren().contains(logTextArea)) {
                            textOrImagePane.getChildren().remove(logTextArea);
                        }
                        if (!textOrImagePane.getChildren().contains(logScrollPane)) {
                            textOrImagePane.getChildren().add(logScrollPane);
                        }

                        Image img = new Image("file:" + this.currentFolder + "\\" + fileModel.getName());
                        logImageView.setImage(img);

                        break;
                    default:
                        System.err.format("'%s' has an" + " unsupported filetype.%n", fileModel.getName());
                }

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
