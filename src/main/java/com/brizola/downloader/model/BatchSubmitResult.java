package com.brizola.downloader.model;

import java.util.List;

public class BatchSubmitResult {
    private List<String> jobIds;
    private List<RejectedUrl> rejected;

    public BatchSubmitResult(List<String> jobIds, List<RejectedUrl> rejected) {
        this.jobIds = jobIds;
        this.rejected = rejected;
    }

    public List<String> getJobIds() { return jobIds; }
    public List<RejectedUrl> getRejected() { return rejected; }
}