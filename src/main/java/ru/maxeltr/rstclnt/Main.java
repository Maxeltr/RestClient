/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.maxeltr.rstclnt;

import java.net.URL;
import ru.maxeltr.rstclnt.Config.AppConfig;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.maxeltr.rstclnt.Controller.FXMLController;

/**
 *
 * @author Maxim Eltratov <Maxim.Eltratov@yandex.ru>
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
//        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Scene.fxml"));

        ConfigurableApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
        FXMLController fxmlController = applicationContext.getBean(FXMLController.class);
        URL location = getClass().getResource("/fxml/Scene.fxml");
        FXMLLoader loader = new FXMLLoader(location);
        loader.setController(fxmlController);

        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add("/styles/Styles.css");

        stage.setTitle("JavaFX and Gradle");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
