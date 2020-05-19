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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import ru.maxeltr.rstclnt.Config.Config;
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
            String dir = new String(this.crypter.decrypt(this.config.getProperty("LogDir", "")));
            this.currentLogDir = new File(dir);
        } else {
            this.currentLogDir = new File(System.getProperty("user.home"));
        }
    }

    public void encipherFile(File file) throws IOException {
        byte[] data = this.readBytes(file);
        if (!this.crypter.isInitialized()) {
            this.crypter.initialize();
        }
        byte[] key2 = this.crypter.decrypt(this.config.getProperty("Key2", ""));
        String encrypted = this.crypter.encrypt(data, new String(key2).toCharArray());
        Path filePath = Paths.get(file.getPath());
        Files.write(filePath, encrypted.getBytes());
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
                    fileType = this.getFileType(file.getCanonicalPath());
                    if (!fileType.equals("image/jpeg") && !fileType.equals("text/plain")) {
                        Logger.getLogger(FileService.class.getName()).log(Level.WARNING, String.format("'%s' has an" + " unsupported filetype.%n", file.toPath()));
                        continue;
                    }

                    FileModel fileModel = new FileModel();
                    fileModel.setFilename(fileName);
                    fileModel.setDate(sdf.format(file.lastModified()));
                    fileModel.setSize("" + file.length());
                    fileModel.setType(fileType);
                    items.add(fileModel);
                } catch (IOException ex) {
                    Logger.getLogger(FileService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return items;
    }

    public String getFileType(String filePathName) {
        String fileType;
        try {
            Path path = new File(filePathName).toPath();
            fileType = Files.probeContentType(path);
            if (fileType == null) {
                if (filePathName.toLowerCase().endsWith(".log")) {
                    fileType = "text/plain";
                } else {
                    fileType = "";
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(FileService.class.getName()).log(Level.SEVERE, null, ex);
            fileType = "";
        }

        return fileType;
    }

    public Map<byte[], String> decryptText(FileModel fileModel) {
        File file = new File(this.currentLogDir, fileModel.getFilename());
        if (!file.exists()) {
            return null;
        }

        byte[] data = this.readBytes(file);
        if (data.length == 0) {
            Logger.getLogger(FileService.class.getName()).log(Level.WARNING, String.format("Cannot read file: %s.%n", this.currentLogDir + File.separator + fileModel.getFilename()));
            return null;
        }

        if (!this.crypter.isInitialized()) {
            this.crypter.initialize();
        }

        byte[] key, key2, keyPhrase, prefix, decrypted;
        key = this.crypter.decrypt(this.config.getProperty("Key", ""));
        key2 = this.crypter.decrypt(this.config.getProperty("Key2", ""));
        prefix = this.crypter.decrypt(this.config.getProperty("Prefix", ""));
        keyPhrase = this.crypter.decrypt(this.config.getProperty("KeyPhrase", ""));

        String runs = "3pkk2";
        decrypted = this.crypter.decrypt(new String(data), new String(key2).toCharArray());
        decrypted = this.crypter.decode(this.crypter.decode(decrypted, key), prefix);
        if (!matchArrayBeginings(decrypted, keyPhrase)) {
            runs = "2pk2";
            decrypted = this.crypter.decrypt(new String(data), new String(key2).toCharArray());
            decrypted = this.crypter.decode(decrypted, prefix);
            if (!matchArrayBeginings(decrypted, keyPhrase)) {
                runs = "2pk";
                decrypted = this.crypter.decode(this.crypter.decode(data, key), prefix);
                if (!matchArrayBeginings(decrypted, keyPhrase)) {
                    runs = "1p";
                    decrypted = this.crypter.decode(data, prefix);
                }
            }
        }

        Map decryptedData = new HashMap<>();
        decryptedData.put("data", decrypted);
        decryptedData.put("runs", runs);

        return decryptedData;
    }

    public Image getImage(FileModel fileModel) {
        File file = new File(this.currentLogDir, fileModel.getFilename());
        if (!file.exists()) {
            //throw exception
        }

        byte[] data = this.readBytes(file);
        if (data.length == 0) {
            Logger.getLogger(FileService.class.getName()).log(Level.WARNING, String.format("Cannot read file: %s.%n", this.currentLogDir + File.separator + fileModel.getFilename()));
            //throw exception
        }

        if (!this.crypter.isInitialized()) {
            this.crypter.initialize();
        }

        byte[] key, key2, decrypted;
        key = this.crypter.decrypt(this.config.getProperty("Key", ""));
        key2 = this.crypter.decrypt(this.config.getProperty("Key2", ""));

        decrypted = this.crypter.decrypt(new String(data), new String(key2).toCharArray());
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

    public byte[] readBytes(File file) {
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
