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
package ru.maxeltr.rstclnt.Controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import ru.maxeltr.rstclnt.Config.AppConfig;
import ru.maxeltr.rstclnt.Config.Config;
import ru.maxeltr.rstclnt.Service.Crypter;

/**
 *
 * @author Maxim Eltratov <Maxim.Eltratov@yandex.ru>
 */
public class PinController extends AbstractController implements Initializable {

    @FXML
    private Parent root;

    @FXML
    private PasswordField pinField;

    @FXML
    private Button okButton;

    @FXML
    private Button cancelButton;

    private char[] pin = {};

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    public PinController() {

    }

    public void clearPin() {
        for (int i = 0; i < this.pin.length; i++) {
            this.pin[i] = 0;
        }
        this.pin = new char[0];
    }

    public char[] getPin() {
        return this.pin;
    }

    public Boolean initialize() {
        try {
            this.showPinForm();
        } catch (IOException ex) {
            Logger.getLogger(PinController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return this.isInitialized();
    }

    public Boolean isInitialized() {
        return (this.pin.length != 0);
    }

    private void showPinForm() throws IOException {
        Scene scene = new Scene(this.loadView("/fxml/Pin.fxml", this));
        scene.getStylesheets().add(AppConfig.CSS_PATHNAME);
        Stage stage = this.createStage(scene, "PIN", this.root.getScene().getWindow(), Modality.WINDOW_MODAL);
        stage.showAndWait();
    }

    @FXML
    private void handleOkButton(ActionEvent event) {
        this.pin = this.pinField.getText().toCharArray();
        this.pinField.clear();
        Stage stage = (Stage) this.okButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleCancelButton(ActionEvent event) {
        this.pinField.clear();
        Stage stage = (Stage) this.cancelButton.getScene().getWindow();
        stage.close();
    }
}
