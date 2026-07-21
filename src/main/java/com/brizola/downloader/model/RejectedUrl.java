package com.brizola.downloader.model;

public class RejectedUrl {
    private String url;
    private String reason;

    public RejectedUrl(String url, String reason) {
        this.url = url;
        this.reason = reason;
    }

    public String getUrl() { return url; }
    public String getReason() { return reason; }
}