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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import org.springframework.web.client.RestTemplate;
import ru.maxeltr.rstclnt.Config.AppConfig;
import ru.maxeltr.rstclnt.Config.Config;
import ru.maxeltr.rstclnt.Crypter;
import ru.maxeltr.rstclnt.Model.FileModel;

public class FileService {

    private File currentLogDir;

    private final Crypter crypter;

    private final Config config;

    public FileService(Config config, Crypter crypter) {
        this.config = config;
        this.crypter = crypter;

        if (!this.crypter.isInitialized()) {
            this.crypter.initialize();
        }

        if (this.crypter.isInitialized()) {
            String dir = this.crypter.decrypt(this.config.getProperty("LogDir", "")).toString();
            this.currentLogDir = new File(dir);
        } else {
            this.currentLogDir = new File(System.getProperty("user.home"));
        }
    }

    public void encipherFile(File file) throws UnsupportedEncodingException, IOException {
        byte[] data = this.readBytes(file);
        if (!this.crypter.isInitialized()) {
            this.crypter.initialize();
        }
        byte[] key = this.crypter.decrypt(this.config.getProperty("Key", ""));
        String encrypted = this.crypter.encrypt(data, new String(key, AppConfig.DEFAULT_ENCODING).toCharArray());
        Path filePath = Paths.get(file.getPath());
        Files.write(filePath, encrypted.getBytes(AppConfig.DEFAULT_ENCODING));
    }

    public File getCurrentLogDir() {
        return this.currentLogDir;
    }

    public FileService setCurrentLogDir(File folder) {
        this.currentLogDir = folder;

        return this;
    }

    public ObservableList<FileModel> getLocalFiles() {
        ObservableList<FileModel> items = FXCollections.observableArrayList();
        File[] files = this.currentLogDir.listFiles((File dir, String name1) -> name1.toLowerCase().endsWith(".log") || name1.toLowerCase().endsWith(".jpg"));
        if (files != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            String fileType, fileName;
            for (File file : files) {
                try {
                    fileName = file.getName();
                    fileType = Files.probeContentType(file.toPath());
                    if (fileType == null) {
                        if (fileName.toLowerCase().endsWith(".log")) {
                            fileType = "text/plain";
                        } else {
                            Logger.getLogger(FileService.class.getName()).log(Level.WARNING, String.format("%s has an unknown filetype.", file.toPath()));
                            continue;
                        }
                    } else {
                        if (!fileType.equals("image/jpeg") && !fileType.equals("text/plain")) {
                            Logger.getLogger(FileService.class.getName()).log(Level.WARNING, String.format("'%s' has an" + " unsupported filetype.%n", file.toPath()));
                            continue;
                        }
                    }
                    items.add(new FileModel(fileName, sdf.format(file.lastModified()), "" + file.length(), fileType));
                } catch (IOException ex) {
                    Logger.getLogger(FileService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return items;
    }

    public byte[] getText(FileModel fileModel) throws UnsupportedEncodingException {
        File file = new File(this.currentLogDir, fileModel.getFilename());
        if (!file.exists()) {
            this.messsageNotImplemented();      //download in thread?
        }

        byte[] data = this.readBytes(file);
        if (data.length == 0) {
            Logger.getLogger(FileService.class.getName()).log(Level.WARNING, String.format("Cannot read file: %s.%n", this.currentLogDir + "\\" + fileModel.getFilename()));
            //return;
        }

        if (!this.crypter.isInitialized()) {
            this.crypter.initialize();
        }

        byte[] key, keyPhrase, prefix, decrypted;
        key = this.crypter.decrypt(this.config.getProperty("Key", ""));
        prefix = this.crypter.decrypt(this.config.getProperty("Prefix", ""));
        keyPhrase = this.crypter.decrypt(this.config.getProperty("KeyPhrase", ""));

        decrypted = this.crypter.decrypt(new String(data, AppConfig.DEFAULT_ENCODING), new String(key, AppConfig.DEFAULT_ENCODING).toCharArray());
        decrypted = this.crypter.decode(this.crypter.decode(decrypted, key), prefix);
        if (!matchArrayBeginings(decrypted, keyPhrase)) {
            decrypted = this.crypter.decode(this.crypter.decode(data, key), prefix);
            if (!matchArrayBeginings(decrypted, keyPhrase)) {
                decrypted = this.crypter.decode(data, prefix);
            }
        }

        return decrypted;
    }

    public Image getImage(FileModel fileModel) throws UnsupportedEncodingException {
        File file = new File(this.currentLogDir, fileModel.getFilename());
        if (!file.exists()) {
            this.messsageNotImplemented();      //download in thread
        }

        byte[] data = this.readBytes(file);
        if (data.length == 0) {
            Logger.getLogger(FileService.class.getName()).log(Level.WARNING, String.format("Cannot read file: %s.%n", this.currentLogDir + "\\" + fileModel.getFilename()));
            //return;
        }

        if (!this.crypter.isInitialized()) {
            this.crypter.initialize();
        }

        byte[] key, decrypted;
        key = this.crypter.decrypt(this.config.getProperty("Key", ""));

        decrypted = this.crypter.decrypt(new String(data, AppConfig.DEFAULT_ENCODING), new String(key, AppConfig.DEFAULT_ENCODING).toCharArray());
        decrypted = this.crypter.decode(decrypted, key);

        Image img = new Image(new ByteArrayInputStream(decrypted));
        if (img.isError()) {
            decrypted = this.crypter.decode(data, key);
            img = new Image(new ByteArrayInputStream(decrypted));
        }

        return img;
    }

    private boolean matchArrayBeginings(byte[] largerArray, byte[] smallerArray) {
        return Arrays.equals(smallerArray, Arrays.copyOfRange(largerArray, 0, smallerArray.length));
    }

    private byte[] readBytes(File file) {
        byte[] data = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file);) {
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(data);
        } catch (IOException ex) {
            Logger.getLogger(FileService.class.getName()).log(Level.SEVERE, String.format("Cannot open or read %s.%n", file.toString(), ex));
            data = new byte[0];
        }

        return data;
    }

    private void messsageNotImplemented() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText("Not implemented!");

        alert.showAndWait();
    }
}
