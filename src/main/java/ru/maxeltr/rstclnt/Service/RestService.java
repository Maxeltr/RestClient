/*
 * The MIT License
 *
 * Copyright 2020 Maxim Eltratov <Maxim.Eltratov@yandex.ru>.
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
package ru.maxeltr.rstclnt.Service;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.maxeltr.rstclnt.Config.AppConfig;
import ru.maxeltr.rstclnt.Config.Config;
import ru.maxeltr.rstclnt.Crypter;
import ru.maxeltr.rstclnt.Model.FileModel;
import ru.maxeltr.rstclnt.Model.ResponseFileData;

public class RestService {

    private final Crypter crypter;

    private final Config config;

    private String accessToken = "";
    private String expiresIn = "";
    private String tokenType = "";
    private String scope = "";

    public RestService(Config config, Crypter crypter) {
        this.config = config;
        this.crypter = crypter;

    }

    public ObservableList<FileModel> getListRemoteFiles() {
        ObservableList<FileModel> items = FXCollections.observableArrayList();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + this.getToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<ResponseFileData> response = restTemplate.exchange(AppConfig.URL_GET_FILES, HttpMethod.GET, entity, ResponseFileData.class);
        FileModel[] files = response.getBody().getFileList().getFiles();

        items.addAll(Arrays.asList(files));
        return items;
    }

    public String getToken() {
        if (!this.accessToken.isEmpty()) {
            return this.accessToken;
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (this.crypter.isInitialized()) {

            try {
                Map<String, String> body = new HashMap<>();
                body.put("grant_type", new String(this.crypter.decrypt(this.config.getProperty("GrantTypes", "")), AppConfig.DEFAULT_ENCODING));
                body.put("client_secret", new String(this.crypter.decrypt(this.config.getProperty("ClientSecret", "")), AppConfig.DEFAULT_ENCODING));
                body.put("client_id", new String(this.crypter.decrypt(this.config.getProperty("ClientId", "")), AppConfig.DEFAULT_ENCODING));

                HttpEntity<Map> requestEntity = new HttpEntity<>(body, headers);

                ResponseEntity<Map> response = restTemplate.exchange(AppConfig.URL_GET_TOKEN, HttpMethod.POST, requestEntity, Map.class);

//                System.out.println(response.getBody());

                this.accessToken = response.getBody().get("access_token").toString();
                this.expiresIn = response.getBody().get("expires_in").toString();
                this.tokenType = response.getBody().get("token_type").toString();
                this.scope = response.getBody().get("scope").toString();

            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        return this.accessToken;
    }

    public String refreshToken() {
        return null;
    }

    public void downloadRemoteFile() {

    }

    public void downloadRemoteFiles() {

    }

    private void messsageNotImplemented() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText("Not implemented!");

        alert.showAndWait();
    }
}
