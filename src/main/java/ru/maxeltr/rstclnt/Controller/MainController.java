package ru.maxeltr.rstclnt.Controller;

import Service.FileService;
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
import javafx.scene.input.ScrollEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javax.imageio.ImageIO;
import ru.maxeltr.rstclnt.Config.AppConfig;
import ru.maxeltr.rstclnt.Config.Config;
import ru.maxeltr.rstclnt.Crypter;
import ru.maxeltr.rstclnt.Model.FileModel;
import org.springframework.web.client.RestTemplate;

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

    private File currentFolder;

    private ListView<String> textWin;

    private ImageView logImageView;

    private ScrollPane imgWin;

    private double currentScale = 1;
    private final double SCALE_MIN = 0.1;
    private final double SCALE_MAX = 5;

    private final OptionController optionController;
    private final FileService fileService;
    private final Crypter crypter;
    private final Config config;

    public MainController(FileService fileService, OptionController optionController, Crypter crypter, Config config) {
        this.fileService = fileService;
        this.optionController = optionController;
        this.config = config;
        this.crypter = crypter;
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

        if (!this.crypter.isInitialized()) {
            this.crypter.initialize();
        }

        if (this.crypter.isInitialized()) {
            String dir = this.crypter.decrypt(this.config.getProperty("LogDir", "")).toString();
            this.currentFolder = new File(dir);
            try {
                this.fileTable.setItems(this.makeFileList(this.currentFolder));
            } catch (IOException ex) {
                Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    @FXML
    private void handleEncipherFile(ActionEvent event) throws UnsupportedEncodingException, IOException {
        FileChooser chooser = new FileChooser();
        Window stage = (Stage) root.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        byte[] data = this.readBytes(file);
        if (!this.crypter.isInitialized()) {
            this.crypter.initialize();
        }
        byte[] key = this.crypter.decrypt(this.config.getProperty("Key", ""));
        String encrypted = this.crypter.encrypt(data, new String(key, AppConfig.DEFAULT_ENCODING).toCharArray());
        Path filePath = Paths.get(file.getPath());
        Files.write(filePath, encrypted.getBytes(AppConfig.DEFAULT_ENCODING));
    }

    @FXML
    private void handleChooseFolder(ActionEvent event) throws IOException {
        DirectoryChooser chooser = new DirectoryChooser();
        Window stage = (Stage) root.getScene().getWindow();
        this.currentFolder = chooser.showDialog(stage);
        if (this.currentFolder != null) {
            this.fileTable.getItems().clear();
            this.textWin.getItems().clear();
            this.logImageView.setImage(null);
            this.fileTable.setItems(this.makeFileList(this.currentFolder));
        }
    }

    private ObservableList makeFileList(File folder) throws IOException {
        ObservableList<FileModel> items = FXCollections.observableArrayList();
        File[] files = folder.listFiles((File dir, String name1) -> name1.toLowerCase().endsWith(".log") || name1.toLowerCase().endsWith(".jpg"));
        if (files != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            String fileType, fileName;
            for (File file : files) {
                fileName = file.getName();
                fileType = Files.probeContentType(file.toPath());
                if (fileType == null) {
                    if (fileName.toLowerCase().endsWith(".log")) {
                        fileType = "text/plain";
                    } else {
                        Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("%s has an unknown filetype.", file.toPath()));
                        continue;
                    }
                } else {
                    if (!fileType.equals("image/jpeg") && !fileType.equals("text/plain")) {
                        Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("'%s' has an" + " unsupported filetype.%n", file.toPath()));
                        continue;
                    }
                }
                items.add(new FileModel(fileName, sdf.format(file.lastModified()), "" + file.length(), fileType));
            }
        }

        return items;
    }

    @FXML
    private void handleFileTableClicked(MouseEvent event) throws UnsupportedEncodingException {
        if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
            FileModel fileModel = this.fileTable.getSelectionModel().getSelectedItem();
            if (fileModel == null || this.currentFolder == null) {
                Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("Current folder or file model is null.%n"));
                return;
            }
            File file = new File(this.currentFolder, fileModel.getFilename());  //TODO file not found
            if (!file.exists()) {                                                 //add
                this.messsageNotImplemented(); //download in thread
            }

            byte[] data = this.readBytes(file);
            if (data.length == 0) {
                Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("Cannot read file: %s.%n", this.currentFolder + "\\" + fileModel.getFilename()));
                return;
            }

            if (!this.crypter.isInitialized()) {
                this.crypter.initialize();
            }
            byte[] key, keyPhrase, prefix, decrypted;
            String codePage;
            key = this.crypter.decrypt(this.config.getProperty("Key", ""));
            prefix = this.crypter.decrypt(this.config.getProperty("Prefix", ""));
            keyPhrase = this.crypter.decrypt(this.config.getProperty("KeyPhrase", ""));
            codePage = this.config.getProperty("CodePage", AppConfig.DEFAULT_ENCODING);

            switch (fileModel.getType()) {
                case ("text/plain"):
                    this.changeToTexWin();

//                    try() {
                    decrypted = this.crypter.decrypt(new String(data, AppConfig.DEFAULT_ENCODING), new String(key, AppConfig.DEFAULT_ENCODING).toCharArray());
//                    } catch (IOException ex) {

//                    }
                    decrypted = this.crypter.decode(this.crypter.decode(decrypted, key), prefix);
                    if (!matchArrayBeginings(decrypted, keyPhrase)) {
                        decrypted = this.crypter.decode(this.crypter.decode(data, key), prefix);
                        if (!matchArrayBeginings(decrypted, keyPhrase)) {
                            decrypted = this.crypter.decode(data, prefix);
                        }
                    }

                    String str = new String(decrypted, codePage);

                    ObservableList<String> lvItems = FXCollections.observableArrayList();
                    for (String s : str.split(System.lineSeparator())) {
                        lvItems.add(s);
                    }

                    this.textWin.getItems().clear();
                    this.textWin.setItems(lvItems);

                    break;
                case ("image/jpeg"):
                    this.changeToImgWin();

                    decrypted = this.crypter.decrypt(new String(data, AppConfig.DEFAULT_ENCODING), new String(key, AppConfig.DEFAULT_ENCODING).toCharArray());
                    decrypted = this.crypter.decode(decrypted, key);

                    Image img = new Image(new ByteArrayInputStream(decrypted));
                    if (img.isError()) {
                        decrypted = this.crypter.decode(data, key);
                        img = new Image(new ByteArrayInputStream(decrypted));
                    }
                    this.logImageView.setImage(img);

                    break;
                default:
                    Logger.getLogger(MainController.class.getName()).log(Level.WARNING, String.format("%s has an unsupported file type.%n", this.currentFolder + "\\" + fileModel.getFilename()));
            }

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
        RestTemplate restTemplate = new RestTemplate();
        String files = restTemplate.getForObject(AppConfig.URL_GET_FILES, String.class);

        Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, files);
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
