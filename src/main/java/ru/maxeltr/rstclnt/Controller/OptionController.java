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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
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
public class OptionController implements Initializable {

    @FXML
    private Parent root;

    @FXML
    private TextField prefixField;

    @FXML
    private TextField keyField;

    @FXML
    private TextField keyPhraseField;

    @FXML
    private TextField codePageField;

    @FXML
    private TextField logDirField;

    @FXML
    private TextField clientSecretField;

    @FXML
    private TextField clientIdField;

    @FXML
    private TextField key2Field;

    @FXML
    private Button cancelOptionsButton;

    @FXML
    private Button saveOptionsButton;

    private final Crypter crypter;

    private final Config config;

    public OptionController(Config config, Crypter crypter) {
        this.config = config;
        this.crypter = crypter;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.getNonEncryptedSettings();
        this.getEncryptedSettings();
    }

    @FXML
    private void handleSaveOptions(ActionEvent event) {
        this.saveNonEncryptedSettings();
        this.saveEncryptedSettings();

        Stage stage = (Stage) this.saveOptionsButton.getScene().getWindow();
        stage.close();
    }

    private void saveEncryptedSettings() {
        if (!this.crypter.isInitialized()) {
            if (!this.crypter.initialize()) {
                return;
            }
        }

        this.config.setProperty("Prefix", this.crypter.encrypt(this.prefixField.getText().getBytes()));
        this.config.setProperty("Key", this.crypter.encrypt(this.keyField.getText().getBytes()));
        this.config.setProperty("KeyPhrase", this.crypter.encrypt(this.keyPhraseField.getText().getBytes()));
        this.config.setProperty("LogDir", this.crypter.encrypt(this.logDirField.getText().getBytes()));
        this.config.setProperty("ClientSecret", this.crypter.encrypt(this.clientSecretField.getText().getBytes()));
        this.config.setProperty("ClientId", this.crypter.encrypt(this.clientIdField.getText().getBytes()));
        this.config.setProperty("Key2", this.crypter.encrypt(this.key2Field.getText().getBytes()));
    }

    private void saveNonEncryptedSettings() {
        this.config.setProperty("CodePage", this.codePageField.getText());
    }

    private void getEncryptedSettings() {
        if (!this.crypter.isInitialized()) {
            if (!this.crypter.initialize()) {
                return;
            }
        }

        this.prefixField.setText(new String(this.crypter.decrypt(this.config.getProperty("Prefix", ""))));
        this.keyField.setText(new String(this.crypter.decrypt(this.config.getProperty("Key", ""))));
        this.keyPhraseField.setText(new String(this.crypter.decrypt(this.config.getProperty("KeyPhrase", ""))));
        this.logDirField.setText(new String(this.crypter.decrypt(this.config.getProperty("LogDir", ""))));
        this.clientSecretField.setText(new String(this.crypter.decrypt(this.config.getProperty("ClientSecret", ""))));
        this.clientIdField.setText(new String(this.crypter.decrypt(this.config.getProperty("ClientId", ""))));
        this.key2Field.setText(new String(this.crypter.decrypt(this.config.getProperty("Key2", ""))));
    }

    private void getNonEncryptedSettings() {
        this.codePageField.setText(this.config.getProperty("CodePage", ""));
    }

    @FXML
    private void handleCancelOptions(ActionEvent event) {
        Stage stage = (Stage) this.cancelOptionsButton.getScene().getWindow();
        stage.close();
    }
}
