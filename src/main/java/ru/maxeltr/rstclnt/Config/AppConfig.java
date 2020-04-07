/*
 * The MIT License
 *
 * Copyright 2019 Maxim Eltratov <Maxim.Eltratov@yandex.ru>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ru.maxeltr.rstclnt.Config;

import ru.maxeltr.rstclnt.Service.FileService;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.logging.LogManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javax.crypto.NoSuchPaddingException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.maxeltr.rstclnt.Controller.MainController;
import ru.maxeltr.rstclnt.Controller.OptionController;
import ru.maxeltr.rstclnt.Controller.PinController;
import ru.maxeltr.rstclnt.Service.Crypter;
import ru.maxeltr.rstclnt.Service.RestService;

/**
 *
 * @author Maxim Eltratov <Maxim.Eltratov@yandex.ru>
 */
@Configuration
public class AppConfig {
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String CONFIG_PATHNAME = "Configuration.xml";
    public static final String CSS_PATHNAME = "/styles/Styles.css";
    public static final byte[] SALT = {1, 2, 3, 4, 5, 6, 7, 8};
    public static final int ITERATION_COUNT = 4000;
    public static final int KEY_LENGTH = 128;

    public static final String URL_GET_FILES = "http://176.113.82.112/v1/api/file";
    public static final String URL_GET_FILE = "http://176.113.82.112/v1/api/file";
    public static final String URL_GET_TOKEN = "http://176.113.82.112/oauth";
    public static final String URL_UPLOAD_FILE = "http://176.113.82.112/api/file";
    public static final String URL_DELETE_FILE = "http://176.113.82.112/api/file";

    public AppConfig() {
        try {
            LogManager.getLogManager().readConfiguration(
                    AppConfig.class.getResourceAsStream("/logging.properties")
            );
        } catch (IOException | SecurityException ex) {
            System.err.println("Could not setup logger configuration: " + ex.toString());
        }
    }

    @Bean
    public Config config() {
        return new Config(CONFIG_PATHNAME);
    }

    @Bean
    public Crypter crypter(PinController pinController) throws NoSuchAlgorithmException, NoSuchPaddingException {
        return new Crypter(pinController);
    }

    @Bean
    public FileService fileService(Config config, Crypter crypter) {
        return new FileService(config, crypter);
    }

    @Bean
    public RestService restService(Config config, Crypter crypter) {
        return new RestService(config, crypter);
    }

    @Bean
    public OptionController optionController(Config config, Crypter crypter) {
        return new OptionController(config, crypter);
    }

    @Bean
    public MainController mainController(FileService fileService, RestService restService, OptionController optionController, Config config) {
        return new MainController(fileService, restService, optionController, config);
    }

    @Bean
    public PinController pinController() {
        return new PinController();
    }

    @Bean(name = "mainView")
    public Parent getMainView(MainController mainController) throws IOException {
        return loadView("/fxml/Main.fxml", mainController);
    }

//    @Bean(name = "pinView")
//    public Parent getPinView(PinController pinController) throws IOException {
//        return loadView("/fxml/Pin.fxml", pinController);
//    }

    private Parent loadView(String fxml, Object controller) throws IOException {
        URL location = getClass().getResource(fxml);
        FXMLLoader loader = new FXMLLoader(location);
        loader.setController(controller);
        return loader.load();
    }
}
