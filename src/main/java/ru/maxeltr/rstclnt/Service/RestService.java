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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
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

    public ObservableList<FileModel> getListRemoteFiles(String page) {
        ObservableList<FileModel> items = FXCollections.observableArrayList();

        if (!this.crypter.isInitialized()) {
            if (!this.crypter.initialize()) {
                return items;
            } else {
                this.authenticate();
            }
        }

        String url = page.isEmpty() ? AppConfig.URL_GET_FILES : AppConfig.URL_GET_FILES + "?page=" + page;
        HttpEntity<String> requestEntity;
        ResponseEntity<ResponseFileData> response;
        RestTemplate restTemplate = new RestTemplate();
        requestEntity = new HttpEntity<>(this.buildAuthorizationHeaders());

        try {
            response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, ResponseFileData.class);
        } catch (HttpClientErrorException ex) {
            Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, "Cannot connect to server. Token has expired may be. Let's try to authenticate.", ex);
            this.authenticate();
            requestEntity = new HttpEntity<>(this.buildAuthorizationHeaders());
            try {
                response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, ResponseFileData.class);   //TODO delete this
            } catch (HttpStatusCodeException e) {
                Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, "Cannot connect to server. May be re-authentication failed.", e);

                return items;
            }
        } catch (HttpStatusCodeException ex) {
            Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, "Cannot connect to server.", ex);

            return items;
        }

        this.responseFileData = response.getBody();
        FileModel[] files = this.responseFileData.getFileList().getFiles();
        items.addAll(Arrays.asList(files));

        return items;
    }

    public File downloadFile(FileModel fileModel, File dir) {
        if (fileModel.getFileId() == null) {
            Logger.getLogger(RestService.class.getName()).log(Level.WARNING, String.format("Cannot download file, because id is null.%n"));
            return null;
        }
        String filename = fileModel.getFilename();
        String prefix = filename.substring(0, filename.lastIndexOf('.'));
        String url = AppConfig.URL_GET_FILE + "/" + fileModel.getFileId();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer ");
        headers.set("access_token", this.getToken());
        headers.set("d", "1");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        builder.queryParams(requestEntity.getHeaders());
        RestTemplate restTemplate = new RestTemplate();

        File source, destination;
        try {
            source = restTemplate.execute(builder.toUriString(), HttpMethod.GET, null, response -> {
                File tempFile;
                try {
                    tempFile = File.createTempFile(prefix, null, dir);
                } catch (IOException ex) {
                    Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, String.format("Cannot create temp file in dir: %s.%n", dir.getAbsolutePath()), ex);
                    return null;
                }
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    StreamUtils.copy(response.getBody(), out);
                }

                return tempFile;
            });
        } catch (HttpStatusCodeException ex) {
            Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, String.format("Cannot download file: %s.%n", filename), ex);

            return null;
        }

        if (source == null) {
            return null;
        }

        try {
            destination = new File(dir.getCanonicalPath() + File.separator + filename);
            Files.move(source.toPath(), destination.toPath());
        } catch (IOException ex) {
            Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, null, ex);

            return null;
        }

        return destination;
    }

    public void uploadFile(String filename, byte[] file, String description) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", "Bearer " + this.getToken());

        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap();
        ContentDisposition contentDisposition = ContentDisposition
                .builder("form-data")
                .name("file")
                .filename(filename)
                .build();
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        HttpEntity<byte[]> fileEntity = new HttpEntity(file, fileMap);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap();
        body.add("file", fileEntity);
        body.add("description", description);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    AppConfig.URL_UPLOAD_FILE,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
        } catch (HttpClientErrorException ex) {
            Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, String.format("Cannot upload file: %s.%n", filename), ex);
        }
    }

    public ObservableList<FileModel> getNextPage() {
        ObservableList<FileModel> items = FXCollections.observableArrayList();
        if (this.responseFileData == null) {
            return items;
        }

        int currentPage, pageCount;
        try {
            currentPage = Integer.parseInt(this.responseFileData.getPage());
            pageCount = Integer.parseInt(this.responseFileData.getPageCount());
        } catch (NumberFormatException ex) {
            Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, "Cannot parse current page or page count to int.", ex);

            return items;
        }

        currentPage++;
        if (currentPage > pageCount) {
            return items;
        }

        return this.getListRemoteFiles(Integer.toString(currentPage));
    }

    public ObservableList<FileModel> getPrevPage() {
        ObservableList<FileModel> items = FXCollections.observableArrayList();
        if (this.responseFileData == null) {
            return items;
        }

        int currentPage;
        try {
            currentPage = Integer.parseInt(this.responseFileData.getPage());
        } catch (NumberFormatException ex) {
            Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, "Cannot parse current page or page count to int.", ex);

            return items;
        }

        currentPage--;
        if (currentPage < 1) {
            return items;
        }

        return this.getListRemoteFiles(Integer.toString(currentPage));
    }

    private HttpHeaders buildAuthorizationHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + this.getToken());

        return headers;
    }

    public String getCurrentPage() {
        return this.responseFileData.getPage();
    }

    public String getTotalPages() {
        return this.responseFileData.getPageCount();
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
            body.put("grant_type", "client_credentials");
            body.put("client_secret", new String(this.crypter.decrypt(this.config.getProperty("ClientSecret", "")), AppConfig.DEFAULT_ENCODING));
            body.put("client_id", new String(this.crypter.decrypt(this.config.getProperty("ClientId", "")), AppConfig.DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, "Cannot get client credentials from config.", ex);

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
            Logger.getLogger(RestService.class.getName()).log(Level.SEVERE, "Cannot authenticate.", ex);

            return this.accessToken;
        }

        this.accessToken = response.getBody().get("access_token").toString();
        this.expiresIn = response.getBody().get("expires_in").toString();
        this.tokenType = response.getBody().get("token_type").toString();
        this.scope = response.getBody().get("scope").toString();

        return this.accessToken;
    }

    private void messsageNotImplemented() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText("Not implemented!");

        alert.showAndWait();
    }
}
