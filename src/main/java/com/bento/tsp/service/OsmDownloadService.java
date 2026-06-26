package com.bento.tsp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
public class OsmDownloadService {

    private static final Logger log = LoggerFactory.getLogger(OsmDownloadService.class);
    private static final int BUFFER_SIZE = 64 * 1024;

    public void download(String url, Path destination) throws IOException {
        log.info("Downloading OSM file from {} to {}", url, destination);
        Files.createDirectories(destination.getParent());

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(600_000);
        conn.connect();

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("Download failed: HTTP " + status + " from " + url);
        }

        long totalBytes = conn.getContentLengthLong();
        String totalMb = totalBytes > 0 ? String.format("%.0f", totalBytes / 1_048_576.0) : "?";

        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(destination,
                     StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long downloaded = 0;
            int lastLoggedPct = -5;
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                downloaded += read;
                if (totalBytes > 0) {
                    int pct = (int) (downloaded * 100 / totalBytes);
                    if (pct - lastLoggedPct >= 5) {
                        lastLoggedPct = pct;
                        log.info("OSM download: {}% ({} / {} MB)", pct,
                                String.format("%.0f", downloaded / 1_048_576.0), totalMb);
                    }
                }
            }
        }
        log.info("OSM download complete: {}", destination);
    }
}
