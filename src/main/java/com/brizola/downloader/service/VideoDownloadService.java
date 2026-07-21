package com.brizola.downloader.service;

import com.brizola.downloader.model.BatchSubmitResult;
import com.brizola.downloader.model.DownloadJob;
import com.brizola.downloader.model.JobStatus;
import com.brizola.downloader.model.RejectedUrl;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class VideoDownloadService {

    private static final String DOWNLOAD_DIR = "/tmp/downloads/";
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "youtube.com", "www.youtube.com", "youtu.be",
            "instagram.com", "www.instagram.com"
    );

    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();

    @Autowired
    private JobStore jobStore;

    public BatchSubmitResult submitJobs(List<String> urls) {
        List<String> jobIds = new ArrayList<>();
        List<RejectedUrl> rejected = new ArrayList<>();

        for (String url : urls) {
            String validationError = validate(url);

            if (validationError != null) {
                rejected.add(new RejectedUrl(url, validationError));
                continue;
            }

            String jobId = UUID.randomUUID().toString();
            DownloadJob job = new DownloadJob();
            job.setId(jobId);
            job.setUrl(url);
            job.setStatus(JobStatus.PENDING);
            jobStore.save(job);

            jobIds.add(jobId);
            downloadExecutor.submit(() -> processDownload(job));
        }

        return new BatchSubmitResult(jobIds, rejected);
    }

    private String validate(String url) {
        if (url == null || url.isBlank()) {
            return "URL is empty";
        }
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null || ALLOWED_HOSTS.stream().noneMatch(host::endsWith)) {
                return "Unsupported host: " + host;
            }
        } catch (URISyntaxException e) {
            return "Invalid URL format";
        }
        return null;
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