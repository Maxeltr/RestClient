package ru.maxeltr.rstclnt.Controller;

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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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
import javafx.stage.Modality;
import javax.imageio.ImageIO;
import ru.maxeltr.rstclnt.Config.AppConfig;
import ru.maxeltr.rstclnt.Config.Config;
import ru.maxeltr.rstclnt.Crypter;
import ru.maxeltr.rstclnt.Model.FileModel;

public class MainController extends AbstractController implements Initializable {

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

    private File currentFolder;

    private ListView<String> logListView;

    private ImageView logImageView;

    private ScrollPane logScrollPane;

    private double currentScale = 1;
    private final double SCALE_MIN = 0.1;
    private final double SCALE_MAX = 5;

    private final OptionController optionController;
    private final Crypter crypter;
    private final Config config;

    public MainController(OptionController optionController, Crypter crypter, Config config) {
        this.optionController = optionController;
        this.config = config;
        this.crypter = crypter;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        date.setCellValueFactory(new PropertyValueFactory<>("date"));
        size.setCellValueFactory(new PropertyValueFactory<>("size"));
        type.setCellValueFactory(new PropertyValueFactory<>("type"));

        logListView = new ListView<>();

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
        DirectoryChooser chooser = new DirectoryChooser();
        Window stage = (Stage) root.getScene().getWindow();
        this.currentFolder = chooser.showDialog(stage);
        if (this.currentFolder != null) {
            this.fileTable.getItems().clear();
            this.logListView.getItems().clear();
            this.logImageView.setImage(null);
            File[] files = currentFolder.listFiles((File dir, String name1) -> name1.toLowerCase().endsWith(".log") || name1.toLowerCase().endsWith(".jpg"));
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            String fileType, fileName;
            ObservableList<FileModel> items = FXCollections.observableArrayList();
            for (File file : files) {
                fileName = file.getName();
                fileType = Files.probeContentType(file.toPath());
                if (fileType == null) {
                    if (fileName.toLowerCase().endsWith(".log")) {
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
            this.fileTable.setItems(items);
        }
    }

    @FXML
    private void handleFileTableClicked(MouseEvent event) throws FileNotFoundException, IOException {
        if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
            FileModel fileModel = this.fileTable.getSelectionModel().getSelectedItem();
            if (fileModel != null) {
                File file;
                switch (fileModel.getType()) {
                    case ("text/plain"):
                        if (this.textOrImagePane.getChildren().contains(this.logImageView)) {
                            this.textOrImagePane.getChildren().remove(this.logImageView);
                        }
                        if (!this.textOrImagePane.getChildren().contains(this.logListView)) {
                            this.textOrImagePane.getChildren().add(this.logListView);
                        }
                        this.logListView.getItems().clear();

                        file = new File(this.currentFolder + "\\" + fileModel.getName());
                        try (FileInputStream fis = new FileInputStream(file);) {
                            BufferedInputStream bis = new BufferedInputStream(fis);
                            byte[] data = new byte[(int) file.length()];
                            bis.read(data);

                            String key, keyPhrase, prefix, codePage;
                            byte[] decrypted;
                            if (!this.crypter.isInitialized()) {
                                this.crypter.initialize();
                            }
                            key = this.crypter.decrypt(this.config.getProperty("Key", ""));
                            prefix = this.crypter.decrypt(this.config.getProperty("Prefix", ""));
                            keyPhrase = this.crypter.decrypt(this.config.getProperty("KeyPhrase", ""));
                            codePage = this.config.getProperty("CodePage", "UTF-8");

                            decrypted = this.crypter.decode(data, key.getBytes());
                            decrypted = this.crypter.decode(decrypted, prefix.getBytes());

                            byte[] keyPhraseArray = keyPhrase.getBytes();
                            byte[] decryptedBeginning = Arrays.copyOfRange(decrypted, 0, keyPhraseArray.length);

                            if (!Arrays.equals(keyPhraseArray, decryptedBeginning)) {
                                decrypted = this.crypter.decode(data, prefix.getBytes());
                            }

                            String str = new String(decrypted, codePage);

                            ObservableList<String> lvItems = FXCollections.observableArrayList();
                            for (String s : str.split(System.lineSeparator())) {
                                lvItems.add(s);
                            }

                            this.logListView.setItems(lvItems);

                        } catch (IOException e) {
                            // log error
                        }

//                        try (BufferedReader br = new BufferedReader(new FileReader(this.currentFolder + "\\" + fileModel.getName()))) {
////                        try (BufferedReader br =  new InputStreamReader(new FileInputStream(this.currentFolder + "\\" + fileModel.getName()), Charset.forName("Windows-1251"));
//                            String line, key, keyPhrase, prefix, decrypted;
//                            StringBuilder sb = new StringBuilder();
//                            line = br.readLine();
//                            while (line != null) {
//                                sb.append(line);
//                                sb.append(System.lineSeparator());
//                                //line = line.trim();
//                                //if (! line.equals("")) //lvItems.add(line);
//                                line = br.readLine();
//                            }
//
//                            if (! this.crypter.isInitialized()) this.crypter.initialize();
//                            key = this.crypter.decrypt(this.config.getProperty("Key", ""));
//                            prefix = this.crypter.decrypt(this.config.getProperty("Prefix", ""));
//                            keyPhrase = this.crypter.decrypt(this.config.getProperty("KeyPhrase", ""));
//
////                            decrypted = this.crypter.decrypt(sb.toString(), key.toCharArray());
////                            if (decrypted.isEmpty()) {
//                                decrypted = sb.toString();
////                            }
//                            decrypted = this.crypter.decode(this.crypter.decode(decrypted, key), prefix);
//
//                            if (! decrypted.startsWith(keyPhrase)) {
//                                decrypted = this.crypter.decode(sb.toString(), prefix);
//                            }
//
//                            for (String str: decrypted.split("\\n")) {  //System.lineSeparator()?
//                                lvItems.add(str);
//                            }
//
//                            this.logListView.setItems(lvItems);
//                        }
                        break;
                    case ("image/jpeg"):
                        if (this.textOrImagePane.getChildren().contains(this.logListView)) {
                            this.textOrImagePane.getChildren().remove(this.logListView);
                        }
                        if (!this.textOrImagePane.getChildren().contains(this.logScrollPane)) {
                            this.textOrImagePane.getChildren().add(this.logScrollPane);
                        }

                        BufferedImage bImage2;
                        file = new File(this.currentFolder + "\\" + fileModel.getName());
                        try (FileInputStream fis = new FileInputStream(file);) {
                            BufferedInputStream bis = new BufferedInputStream(fis);
                            byte[] data = new byte[(int) file.length()];
                            bis.read(data);

                            String key, keyPhrase, prefix, codePage;
                            byte[] decrypted;
                            if (!this.crypter.isInitialized()) {
                                this.crypter.initialize();
                            }
                            key = this.crypter.decrypt(this.config.getProperty("Key", ""));
                            prefix = this.crypter.decrypt(this.config.getProperty("Prefix", ""));
                            keyPhrase = this.crypter.decrypt(this.config.getProperty("KeyPhrase", ""));
                            codePage = this.config.getProperty("CodePage", "UTF-8");

                            decrypted = this.crypter.decode(data, key.getBytes());

                            Image img = new Image(new ByteArrayInputStream(decrypted));
                            this.logImageView.setImage(img);

                        } catch (IOException e) {
                            // log error
                        }

//                        Image img = new Image("file:" + this.currentFolder + "\\" + fileModel.getName());
//                        this.logImageView.setImage(decrypted);
                        break;
                    default:
                        System.err.format("'%s' has an" + " unsupported filetype.%n", fileModel.getName());
                }

            }
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
