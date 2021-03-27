/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.maxeltr.rstclnt;

import ru.maxeltr.rstclnt.Config.AppConfig;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 *
 * @author Maxim Eltratov <Maxim.Eltratov@yandex.ru>
 */
@SpringBootApplication
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        ConfigurableApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);

        Scene scene = new Scene((Parent) applicationContext.getBean("mainView"));
        scene.getStylesheets().add(AppConfig.CSS_PATHNAME);

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
