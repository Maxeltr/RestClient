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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 *
 * @author Maxim Eltratov <Maxim.Eltratov@yandex.ru>
 */
public class Config {

    private Properties properties = new Properties();
    private Path path;

    Config() {
        this.path = Paths.get("Config.xml");
        this.readConfigFromFile();
    }

    Config(Path path) {
        this.path = path;
        this.readConfigFromFile();
    }

    public String getProperty(String property, String defaultValue) {
        return this.properties.getProperty(property, defaultValue);
    }

    public void setProperty(String property, String value) {
        this.properties.setProperty(property, value);
        this.saveConfigToFile();
    }

//    public Properties setProperties(Properties properties) {
//        //TODO
//    }

    private void readConfigFromFile() {
        File configFile = new File(this.path.toString());
        try (FileInputStream in = new FileInputStream(configFile);) {
            this.properties.loadFromXML(in);
        } catch (IOException e) {
            System.err.format("Cannot read configuration from file '%s'", this.path + "\\" + configFile.getName());
        }
    }

    private void saveConfigToFile() {
        File configFile = new File(this.path.toString());
        try (FileOutputStream out = new FileOutputStream(configFile);) {
            this.properties.storeToXML(out, "Configuration");
        } catch (IOException e) {
            System.err.format("Cannot save configuration to file '%s'", this.path + "\\" + configFile.getName());
        }
    }
}
