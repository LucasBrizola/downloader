package com.brizola.downloader.model;

import java.util.List;

public class DownloadRequest {
    private List<String> urls;

    public List<String> getUrls() { return urls; }
    public void setUrls(List<String> urls) { this.urls = urls; }
}