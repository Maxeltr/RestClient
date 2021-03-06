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
package ru.maxeltr.rstclnt.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseFileData {

    private String currentPageLink;

    private String firstPageLink;

    private String lastPageLink;

    @JsonProperty("_embedded")
    private FileList fileList;

    @JsonProperty("page_count")
    private String pageCount;

    @JsonProperty("page_size")
    private String pageSize;

    @JsonProperty("total_items")
    private String totalItems;

    private String page;

    @JsonProperty("_links")
    private void unpackNestedLinks(Map<String,Object> links) {
        Map<String,String> self = (Map<String,String>)links.get("self");
        this.currentPageLink = self.get("href");
        Map<String,String> first = (Map<String,String>)links.get("first");
        this.firstPageLink = first.get("href");
        Map<String,String> last = (Map<String,String>)links.get("last");
        this.lastPageLink = last.get("href");
    }

//    public void ResponseData() {
//        this.fileList = new FileList();
//
//    }

    public String getCurrentPageLink() {
        return this.currentPageLink;
    }

    public void setCurrentPageLink(String currentPageLink) {
        this.currentPageLink = currentPageLink;
    }

    public String getFirstPageLink() {
        return this.firstPageLink;
    }

    public void setFirstPageLink(String firstPageLink) {
        this.firstPageLink = firstPageLink;
    }

    public String getLastPageLink() {
        return this.lastPageLink;
    }

    public void setLastPageLink(String lastPageLink) {
        this.lastPageLink = lastPageLink;
    }

    public FileList getFileList() {
        return this.fileList;
    }

    public void setFileList(FileList fileList) {
        this.fileList = fileList;
    }

    public String getPageCount() {
        return this.pageCount;
    }

    public void setPageCount(String pageCount) {
        this.pageCount = pageCount;
    }

    public String getPageSize() {
        return this.pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getTotalItems() {
        return this.totalItems;
    }

    public void setTotalItems(String totalItems) {
        this.totalItems = totalItems;
    }

    public String getPage() {
        return this.page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    @Override
    public String toString() {
        return " currentPage: " + this.getCurrentPageLink() + ", firstPage: " + this.getFirstPageLink() + ", lastPage: " + this.getLastPageLink() + ", page_count: " + this.getPageCount() + ", page_size: " + this.getPageSize() + ", total_items: " + this.getTotalItems() + ", page: " + this.getPage();
    }
}
