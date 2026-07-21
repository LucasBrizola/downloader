package com.brizola.downloader.service;

import com.brizola.downloader.model.DownloadJob;
import com.brizola.downloader.model.JobStatus;
import com.brizola.downloader.repository.JobStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class VideoDownloadService {

    private static final String DOWNLOAD_DIR = "/tmp/downloads/";
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "youtube.com", "www.youtube.com", "youtu.be",
            "instagram.com", "www.instagram.com"
    );

    @Autowired
    private JobStore jobStore;

    public String submitJob(String url) {
        validateUrl(url);

        String jobId = UUID.randomUUID().toString();
        DownloadJob job = new DownloadJob();
        job.setId(jobId);
        job.setUrl(url);
        job.setStatus(JobStatus.PENDING);
        jobStore.save(job);

        // run async
        CompletableFuture.runAsync(() -> processDownload(job));

        return jobId;
    }

    private void validateUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null || ALLOWED_HOSTS.stream().noneMatch(host::endsWith)) {
                throw new IllegalArgumentException("Unsupported host: " + host);
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL");
        }
    }

    private void processDownload(DownloadJob job) {
        job.setStatus(JobStatus.DOWNLOADING);
        try {
            Path outputDir = Paths.get(DOWNLOAD_DIR);
            Files.createDirectories(outputDir);

            String outputTemplate = DOWNLOAD_DIR + job.getId() + ".%(ext)s";

            List<String> command = List.of(
                    "yt-dlp",
                    "-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best",
                    "--merge-output-format", "mp4",
                    "--no-playlist",
                    "-o", outputTemplate,
                    "--print", "after_move:filepath",
                    job.getUrl()
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Download timed out");
            }

            if (process.exitValue() != 0) {
                throw new RuntimeException("yt-dlp error: " + output);
            }

            String[] lines = output.split("\n");
            String filePath = lines[lines.length - 1].trim();

            job.setFilePath(filePath);
            job.setStatus(JobStatus.COMPLETED);

        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
        }
    }
}
