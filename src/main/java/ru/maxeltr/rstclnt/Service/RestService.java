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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.maxeltr.rstclnt.Config.AppConfig;
import ru.maxeltr.rstclnt.Config.Config;
import ru.maxeltr.rstclnt.Model.FileModel;
import ru.maxeltr.rstclnt.Model.ResponseFileData;

public class RestService {

    private final Crypter crypter;

    private final Config config;

    private ResponseFileData responseFileData;

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

        HttpEntity<String> requestEntity;
        ResponseEntity<ResponseFileData> response;
        RestTemplate restTemplate = new RestTemplate();
        requestEntity = new HttpEntity<>(this.buildAuthorizationHeaders());

        try {
            response = restTemplate.exchange(AppConfig.URL_GET_FILES, HttpMethod.GET, requestEntity, ResponseFileData.class);
        } catch (HttpClientErrorException ex) {
            Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, null, ex);
            this.authenticate();
            requestEntity = new HttpEntity<>(this.buildAuthorizationHeaders());
            response = restTemplate.exchange(AppConfig.URL_GET_FILES, HttpMethod.GET, requestEntity, ResponseFileData.class);
        } catch (HttpStatusCodeException ex) {
            Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, null, ex);

            return items;
        }

        this.responseFileData = response.getBody();
        FileModel[] files = this.responseFileData.getFileList().getFiles();
        items.addAll(Arrays.asList(files));

        return items;
    }

    private HttpHeaders buildAuthorizationHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + this.getToken());

        return headers;
    }

    public String getToken() {
        return this.accessToken;
    }

    public String authenticate() {
        this.accessToken = "";
        this.expiresIn = "";
        this.tokenType = "";
        this.scope = "";

        if (!this.crypter.isInitialized()) {
            if (!this.crypter.initialize()) {
                return this.accessToken;
            }
        }

        Map<String, String> body = new HashMap<>();
        try {
            body.put("grant_type", new String(this.crypter.decrypt(this.config.getProperty("GrantTypes", "")), AppConfig.DEFAULT_ENCODING));
            body.put("client_secret", new String(this.crypter.decrypt(this.config.getProperty("ClientSecret", "")), AppConfig.DEFAULT_ENCODING));
            body.put("client_id", new String(this.crypter.decrypt(this.config.getProperty("ClientId", "")), AppConfig.DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, null, ex);

            return this.accessToken;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map> requestEntity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(AppConfig.URL_GET_TOKEN, HttpMethod.POST, requestEntity, Map.class);
        } catch (HttpStatusCodeException ex) {
            Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, null, ex);

            return this.accessToken;
        }

        this.accessToken = response.getBody().get("access_token").toString();
        this.expiresIn = response.getBody().get("expires_in").toString();
        this.tokenType = response.getBody().get("token_type").toString();
        this.scope = response.getBody().get("scope").toString();

        return this.accessToken;
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
