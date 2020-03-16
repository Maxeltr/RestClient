package ru.maxeltr.rstclnt.Controller;

import ru.maxeltr.rstclnt.Service.FileService;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
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
import javax.imageio.ImageIO;
import ru.maxeltr.rstclnt.Config.AppConfig;
import ru.maxeltr.rstclnt.Config.Config;
import ru.maxeltr.rstclnt.Model.FileModel;
import org.springframework.web.client.RestTemplate;
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

//    private File currentFolder;

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

        this.fileTable.setItems(this.fileService.getLocalFiles());
    }

    @FXML
    private void handleEncipherFile(ActionEvent event) throws UnsupportedEncodingException, IOException {
        FileChooser chooser = new FileChooser();
        Window stage = (Stage) root.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        this.fileService.encipherFile(file);
    }

    @FXML
    private void handleChooseFolder(ActionEvent event) throws IOException {
        DirectoryChooser chooser = new DirectoryChooser();
        Window stage = (Stage) root.getScene().getWindow();
        File folder = chooser.showDialog(stage);
        if (folder == null) {
            return;
        }

        this.fileTable.getItems().clear();
        this.textWin.getItems().clear();
        this.logImageView.setImage(null);

        ObservableList files = this.fileService.setCurrentLogDir(folder).getLocalFiles();
        this.fileTable.setItems(files);
    }

    @FXML
    private void handleFileTableClicked(MouseEvent event) throws UnsupportedEncodingException, IOException {
        if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
            FileModel fileModel = this.fileTable.getSelectionModel().getSelectedItem();
            if (fileModel == null) {
                Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("File model is null.%n"));
                return;
            }

            File file = new File(this.fileService.getCurrentLogDir(), fileModel.getFilename());
            if (!file.exists()) {
                Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("Cannot show content of file: %s, because file does not exist on disk. Try to download.%n", file.getCanonicalPath()));
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

                byte[] decrypted = this.fileService.getText(fileModel);

                String str;
                try {
                    str = new String(decrypted, this.config.getProperty("CodePage", AppConfig.DEFAULT_ENCODING));
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

                break;
            case ("image/jpeg"):
                this.changeToImgWin();

                try {
                    Image img = this.fileService.getImage(fileModel);
                    this.logImageView.setImage(img);
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

    private boolean matchArrayBeginings(byte[] largerArray, byte[] smallerArray) {
        return Arrays.equals(smallerArray, Arrays.copyOfRange(largerArray, 0, smallerArray.length));
    }

    private byte[] readBytes(File file) {
        byte[] data = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file);) {
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(data);
        } catch (IOException ex) {
            Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, String.format("Cannot open or read %s.%n", file.toString(), ex));
            data = new byte[0];
        }

        return data;
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
        ObservableList files = this.restService.getListRemoteFiles("1");
        this.fileTable.setItems(files);
    }

    @FXML
    private void handleGetNextPage(ActionEvent event) {
        ObservableList files = this.restService.getNextPage();
        if (!files.isEmpty()) {
            this.fileTable.setItems(files);
        }
    }

    @FXML
    private void handleGetPrevPage(ActionEvent event) {

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
