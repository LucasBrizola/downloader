package com.brizola.downloader.controller;

import com.brizola.downloader.model.DownloadJob;
import com.brizola.downloader.model.DownloadRequest;
import com.brizola.downloader.model.JobStatus;
import com.brizola.downloader.repository.JobStore;
import com.brizola.downloader.service.VideoDownloadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VideoController {

    @Autowired
    private VideoDownloadService downloadService;

    @Autowired
    private JobStore jobStore;

    @PostMapping("/download")
    public ResponseEntity<?> startDownload(@RequestBody DownloadRequest request) {
        try {
            String jobId = downloadService.submitJob(request.getUrl());
            return ResponseEntity.accepted().body(Map.of("jobId", jobId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getStatus(@PathVariable String jobId) {
        DownloadJob job = jobStore.get(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "status", job.getStatus(),
                "error", job.getErrorMessage() == null ? "" : job.getErrorMessage()
        ));
    }

    @GetMapping("/file/{jobId}")
    public ResponseEntity<?> getFile(@PathVariable String jobId) throws IOException {
        DownloadJob job = jobStore.get(jobId);
        if (job == null || job.getStatus() != JobStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("File not ready");
        }

        File file = new File(job.getFilePath());
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource);
    }
}
