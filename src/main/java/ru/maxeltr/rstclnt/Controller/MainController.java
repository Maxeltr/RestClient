package ru.maxeltr.rstclnt.Controller;

import ru.maxeltr.rstclnt.Service.FileService;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import ru.maxeltr.rstclnt.Config.AppConfig;
import ru.maxeltr.rstclnt.Config.Config;
import ru.maxeltr.rstclnt.Model.FileModel;
import ru.maxeltr.rstclnt.Service.RestService;

public class MainController extends AbstractController implements Initializable {

    @FXML
    private Parent root;

    @FXML
    private TableView<FileModel> fileTable;

    @FXML
    private TableColumn<FileModel, Integer> filename;

    @FXML
    private TableColumn<FileModel, String> date;

    @FXML
    private TableColumn<FileModel, String> size;

    @FXML
    private TableColumn<FileModel, String> type;

    @FXML
    private StackPane textOrImagePane;

    @FXML
    private TextField currentPage;

    @FXML
    private Button nextPage;

    @FXML
    private Button prevPage;

    @FXML
    private TextField currentPageField;

    @FXML
    private TextField runsField;

    private ListView<String> textWin;

    private ImageView logImageView;

    private ScrollPane imgWin;

    private double currentScale = 1;
    private final double SCALE_MIN = 0.1;
    private final double SCALE_MAX = 5;

    private final OptionController optionController;
    private final FileService fileService;
    private final RestService restService;
    private final Config config;

    public MainController(FileService fileService, RestService restService, OptionController optionController, Config config) {
        this.fileService = fileService;
        this.restService = restService;
        this.optionController = optionController;
        this.config = config;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filename.setCellValueFactory(new PropertyValueFactory<>("filename"));
        date.setCellValueFactory(new PropertyValueFactory<>("date"));
        size.setCellValueFactory(new PropertyValueFactory<>("size"));
        type.setCellValueFactory(new PropertyValueFactory<>("type"));

        textWin = new ListView<>();
        textWin.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setMinWidth(param.getWidth());
                    setMaxWidth(param.getWidth());
                    setPrefWidth(param.getWidth());
                    setWrapText(true);
                    setText(item);
                }
            }
        });

        logImageView = new ImageView();
        logImageView.setPreserveRatio(true);

        imgWin = new ScrollPane();
        imgWin.setPannable(true);
        imgWin.setContent(new Group(logImageView));

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

        ObservableList files = this.fileService.getLocalFiles();
        if (!files.isEmpty()) {
            this.fileTable.setItems(files);
            this.currentPageField.setText("1");
        }

        MenuItem mi1 = new MenuItem("Delete");
        mi1.setOnAction((ActionEvent event) -> {
            FileModel fileModel = this.fileTable.getSelectionModel().getSelectedItem();
            File file = new File(this.fileService.getCurrentLogDir(), fileModel.getFilename());

            if (file.exists() && !file.delete()) {
                Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("Cannot delete file: %s from disk.%n", file.getName()));

                return;
            }

            if (!this.restService.deleteFile(fileModel)) {
                Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("Cannot delete file: %s from server.%n", fileModel.getFilename()));

                return;
            }

            this.fileTable.getItems().remove(fileModel);
        });

        ContextMenu menu = new ContextMenu();
        menu.getItems().add(mi1);
        this.fileTable.setContextMenu(menu);
    }

    @FXML
    private void handleEncipherFile(ActionEvent event) throws IOException {
        FileChooser chooser = new FileChooser();
        Window stage = root.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        this.fileService.encipherFile(file);
    }

    @FXML
    private void handleChooseFolder(ActionEvent event) throws IOException {
        DirectoryChooser chooser = new DirectoryChooser();
        Window stage = root.getScene().getWindow();
        File folder = chooser.showDialog(stage);
        if (folder == null) {
            return;
        }

        this.fileTable.getItems().clear();
        this.textWin.getItems().clear();
        this.logImageView.setImage(null);

        ObservableList files = this.fileService.setCurrentLogDir(folder).getLocalFiles();
        if (!files.isEmpty()) {
            this.fileTable.setItems(files);
            this.currentPageField.setText("1");
        }
    }

    @FXML
    private void handleFileTableClicked(MouseEvent event) throws UnsupportedEncodingException {
        if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
            FileModel fileModel = this.fileTable.getSelectionModel().getSelectedItem();
            if (fileModel == null) {
                Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("File model is null.%n"));
                return;
            }

            File file = new File(this.fileService.getCurrentLogDir(), fileModel.getFilename());
            if (!file.exists()) {
                Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("Cannot show content of file: %s, because file does not exist on disk. Try to download.%n", file.getName()));
                File downloadfile = this.restService.downloadFile(fileModel, this.fileService.getCurrentLogDir());
                if (downloadfile == null) {
                    return;
                }
            }

            this.viewContent(fileModel);

        }
    }

    private void viewContent(FileModel fileModel) throws UnsupportedEncodingException {
        String fileType = this.fileService.getFileType(this.fileService.getCurrentLogDir() + File.separator + fileModel.getFilename());
        switch (fileType) {
            case ("text/plain"):
                this.changeToTexWin();

                Map decrypted = this.fileService.decryptText(fileModel);
                if (decrypted == null) {
                    Logger.getLogger(MainController.class.getName()).log(Level.WARNING, "Cannot decrypt text.");

                    return;
                }

                byte[] text = (byte[]) decrypted.get("data");

                String str;
                try {
                    str = new String(text, this.config.getProperty("CodePage", AppConfig.DEFAULT_ENCODING));
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, null, ex);

                    return;
                }

                ObservableList<String> lvItems = FXCollections.observableArrayList();
                for (String s : str.split(System.lineSeparator())) {
                    lvItems.add(s);
                }

                this.textWin.getItems().clear();
                this.textWin.setItems(lvItems);
                this.textWin.scrollTo(0);
                this.runsField.setText((String) decrypted.get("runs"));

                break;
            case ("image/jpeg"):
                this.changeToImgWin();

                try {
                    Map data = this.fileService.decryptImage(fileModel);
                    if (data == null) {
                        Logger.getLogger(MainController.class.getName()).log(Level.WARNING, "Cannot decrypt image.");

                        return;
                    }

                    Image img = (Image) data.get("image");
                    this.logImageView.setImage(img);
                    this.runsField.setText((String) data.get("runs"));

                } catch (Exception ex) {
                    Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, null, ex);
                }



                break;
            default:
                Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("%s has an unsupported file type: %s.%n", this.fileService.getCurrentLogDir() + File.separator + fileModel.getFilename(), fileType));
        }
    }

    private void changeToTexWin() {
        if (this.textOrImagePane.getChildren().contains(this.imgWin)) {
            this.textOrImagePane.getChildren().remove(this.imgWin);
        }
        if (!this.textOrImagePane.getChildren().contains(this.textWin)) {
            this.textOrImagePane.getChildren().add(this.textWin);
        }
    }

    private void changeToImgWin() {
        if (this.textOrImagePane.getChildren().contains(this.textWin)) {
            this.textOrImagePane.getChildren().remove(this.textWin);
        }
        if (!this.textOrImagePane.getChildren().contains(this.imgWin)) {
            this.textOrImagePane.getChildren().add(this.imgWin);
        }
    }

    @FXML
    private void handleMenuSettings(ActionEvent event) throws IOException {
        Scene scene = new Scene(this.loadView("/fxml/Options.fxml", this.optionController));
        scene.getStylesheets().add(AppConfig.CSS_PATHNAME);
        Stage stage = this.createStage(scene, "Options", this.root.getScene().getWindow(), Modality.WINDOW_MODAL);
        stage.show();
    }

    @FXML
    private void handleButtonAction(ActionEvent event) {
        this.messsageNotImplemented();
    }

    @FXML
    private void handleConnect(ActionEvent event) {
        this.restService.authenticate();
        ObservableList files = this.restService.getListRemoteFiles("1");
        this.fileTable.setItems(files);
        this.updateCurrentPageNumber();
    }

    @FXML
    private void handleGetNextPage(ActionEvent event) {
        ObservableList files = this.restService.getNextPage();
        if (!files.isEmpty()) {
            this.fileTable.setItems(files);
            this.updateCurrentPageNumber();
        }
    }

    @FXML
    private void handleUploadFile() {
        FileChooser chooser = new FileChooser();
        Window stage = root.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        this.restService.uploadFile(file.getName(), this.fileService.readBytes(file), "upload from rstclnt");

        this.showMessage("Upload complete");
    }

    @FXML
    public void handleDownloadCurrentPageFilesToDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        Window stage = root.getScene().getWindow();
        File folder = chooser.showDialog(stage);
        if (folder == null) {
            return;
        }

        int fileCounter = this.downloadCurrentPageFilesToDir(folder);
        this.showMessage("Downloaded files: " + Integer.toString(fileCounter));
    }

    private int downloadCurrentPageFilesToDir(File dir) {
        int fileCounter = 0;
        for (int i = 0; i < this.fileTable.getItems().size(); i++) {
            FileModel fileModel = this.fileTable.getItems().get(i);
            if (fileModel == null) {
                Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("Cannot download file. File model is null.It will be skipped. %n"));
                continue;
            }

            File file = new File(dir, fileModel.getFilename());
            if (file.exists()) {
                Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("File: %s, exists on disk. Skip to download.%n", file.getName()));
                continue;
            }

            File downloadfile = this.restService.downloadFile(fileModel, dir);
            if (downloadfile == null) {
                Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("Cannot download file: %s. It will be skipped.%n", file.getName()));
                continue;
            }
            fileCounter++;
        }

        return fileCounter;
    }

    @FXML
    public void handleDownloadCurrentPageFiles() {
        int fileCounter = this.downloadCurrentPageFilesToDir(this.fileService.getCurrentLogDir());
        this.showMessage("Downloaded files: " + Integer.toString(fileCounter));
    }

    @FXML
    private void handleGetPrevPage(ActionEvent event) {
        ObservableList files = this.restService.getPrevPage();
        if (!files.isEmpty()) {
            this.fileTable.setItems(files);
            this.updateCurrentPageNumber();
        }
    }

    private void updateCurrentPageNumber() {
        this.currentPageField.setText(this.restService.getCurrentPage() + "/" + this.restService.getTotalPages());
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

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}
