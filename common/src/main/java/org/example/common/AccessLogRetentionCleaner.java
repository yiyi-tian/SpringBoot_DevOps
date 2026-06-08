package org.example.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Stream;

public class AccessLogRetentionCleaner {

    private static final Logger log = LoggerFactory.getLogger(AccessLogRetentionCleaner.class);

    private final AccessLogProperties properties;

    public AccessLogRetentionCleaner(AccessLogProperties properties) {
        this.properties = properties;
    }

    @Scheduled(cron = "0 15 * * * *")
    public void cleanExpiredFiles() {
        if (properties.getLocalRetentionHours() <= 0) {
            return;
        }
        Path dir = Path.of(properties.getOutputDir(), properties.getServiceName());
        if (!Files.isDirectory(dir)) {
            return;
        }
        Instant cutoff = Instant.now().minusSeconds(properties.getLocalRetentionHours() * 3600L);
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .forEach(path -> deleteIfExpired(path, cutoff));
        } catch (IOException e) {
            log.warn("Failed to scan access log directory {}: {}", dir, e.getMessage());
        }
    }

    private void deleteIfExpired(Path file, Instant cutoff) {
        try {
            Instant modified = Files.getLastModifiedTime(file).toInstant();
            if (modified.isBefore(cutoff)) {
                Files.deleteIfExists(file);
                log.info("Deleted expired access log file: {}", file);
            }
        } catch (IOException e) {
            log.warn("Failed to delete access log file {}: {}", file, e.getMessage());
        }
    }
}
