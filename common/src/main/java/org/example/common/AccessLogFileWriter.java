package org.example.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccessLogFileWriter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFileWriter.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AccessLogProperties properties;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "access-log-writer");
        t.setDaemon(true);
        return t;
    });

    public AccessLogFileWriter(AccessLogProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    public void write(AccessLogRecord record) {
        executor.execute(() -> doWrite(record));
    }

    private void doWrite(AccessLogRecord record) {
        try {
            String date = LocalDate.now().format(DATE_FMT);
            String fileName = properties.getFilePattern().replace("{date}", date);
            Path dir = Path.of(properties.getOutputDir(), properties.getServiceName());
            Files.createDirectories(dir);
            Path file = dir.resolve(fileName);
            String line = objectMapper.writeValueAsString(record) + System.lineSeparator();
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("Failed to write access log: {}", e.getMessage());
        }
    }
}
