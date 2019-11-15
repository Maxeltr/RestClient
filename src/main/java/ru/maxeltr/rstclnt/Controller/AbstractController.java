package ru.maxeltr.rstclnt.Controller;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public abstract class AbstractController {

    protected Parent loadView(String fxml, Object controller) throws IOException {
        URL location = getClass().getResource(fxml);
        FXMLLoader loader = new FXMLLoader(location);
        loader.setController(controller);
        return loader.load();
    }

    protected Stage createStage(Scene scene, String title, Window root, Modality modality) {
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle(title);
        stage.initModality(modality);
        stage.initOwner(root);

        return stage;
    }
}