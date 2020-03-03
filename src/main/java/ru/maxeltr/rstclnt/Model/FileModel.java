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
package ru.maxeltr.rstclnt.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Maxim Eltratov <Maxim.Eltratov@yandex.ru>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileModel {

    private SimpleStringProperty filename;
    private SimpleStringProperty date;
    private SimpleStringProperty size;
    private SimpleStringProperty type;

    public FileModel(String filename, String date, String size, String type) {
        this.filename = new SimpleStringProperty(filename);
        this.date = new SimpleStringProperty(date);
        this.size = new SimpleStringProperty(size);
        this.type = new SimpleStringProperty(type);
    }

    public String getFilename() {
        return this.filename.get();
    }

    public void setFilename(String filename) {
        this.filename = new SimpleStringProperty(filename);
    }

    public String getDate() {
        return this.date.get();
    }

    public void setDate(String date) {
        this.date = new SimpleStringProperty(date);
    }

    public String getSize() {
        return this.size.get();
    }

    public void setSize(String size) {
        this.size = new SimpleStringProperty(size);
    }

    public String getType() {
        return this.type.get();
    }

    public void setType(String type) {
        this.type = new SimpleStringProperty(type);
    }
}
