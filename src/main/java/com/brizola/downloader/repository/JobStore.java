package com.brizola.downloader.repository;

import com.brizola.downloader.model.DownloadJob;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class JobStore {
    private final Map<String, DownloadJob> jobs = new ConcurrentHashMap<>();

    public void save(DownloadJob job) { jobs.put(job.getId(), job); }
    public DownloadJob get(String id) { return jobs.get(id); }
}
