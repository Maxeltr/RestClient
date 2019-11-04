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
package ru.maxeltr.rstclnt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import static java.lang.System.in;
import java.net.URL;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Maxim Eltratov <Maxim.Eltratov@yandex.ru>
 */
public class OptionController implements Initializable {

    @FXML
    private ScrollPane root;

    @FXML
    private TextField prefix;

    @FXML
    private TextField key;

    @FXML
    private Button cancelOptions;

    @FXML
    private Button saveOptions;

    private FXMLController parent;

    private Crypter crypter;

    private final String password = "1234";
    private final byte[] salt = "12345678".getBytes();
    private final int iterationCount = 40000;
    private final int keyLength = 128;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Properties props = new Properties();
        File configFile = new File("config.xml");
        try {
            FileInputStream in = new FileInputStream(configFile);
            props.loadFromXML(in);
        } catch (IOException e) {
            System.err.format("Cannot read configuration from file '%s'", configFile.getName());
        }

        String prefixText = props.getProperty("prefixText", "0");
        String keyText = props.getProperty("keyText", "0");

        if (prefixText.equals("0") || keyText.equals("0")) {
            return;
        }



        this.prefix.setText(this.crypter.getPrefix());
        this.key.setText(this.crypter.getKey());

    }

    @FXML
    private void handleSaveOptions(ActionEvent event) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException, GeneralSecurityException, IOException {
        String prefixText = this.prefix.getText();
        String keyText = this.key.getText();


        Properties props = new Properties();

        // Set the properties to be saved
        props.setProperty("prefixText", this.crypter.encrypt(prefixText));
        props.setProperty("keyText", this.crypter.encrypt(keyText));

        File configFile = new File("config.xml");
        try {

            FileOutputStream out = new FileOutputStream(configFile);
            props.storeToXML(out, "Configuration");

        } catch (IOException e) {
            System.err.format("Cannot save configuration to file '%s'", configFile.getName());

        }

        Stage stage = (Stage) this.saveOptions.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleCancelOptions(ActionEvent event) {
        Stage stage = (Stage) this.cancelOptions.getScene().getWindow();
        stage.close();
    }

    private void messsageNotImplemented() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("controller 2");
        alert.setHeaderText(null);
        alert.setContentText("Not implemented!");

        alert.showAndWait();
    }

    public void initData(FXMLController controller, Crypter crypter) {
        this.parent = controller;
    }

}
