package com.example.log_receiver.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

@Service
public class JsonWriterService {

    private final Path logDir;
    private final ObjectMapper json =
            new ObjectMapper().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

    public JsonWriterService(@Value("${log-dir:logs}") String dir) throws IOException {
        this.logDir = Paths.get(dir).toAbsolutePath();
        Files.createDirectories(this.logDir);
    }

    @PostConstruct
    void init() throws IOException {
        // каталог уже создан в конструкторе — метод можно оставить пустым
    }

    public synchronized void appendRecord(String type, Object payload) throws IOException {
        Path file = logDir.resolve("%s-%s.log".formatted(type, LocalDate.now()));

        if (Files.notExists(file)) {
            Files.writeString(file, "0", UTF_8);
        }

        List<String> lines = Files.readAllLines(file, UTF_8);
        int counter = Integer.parseInt(lines.getFirst().trim()) + 1;
        lines.set(0, String.valueOf(counter));
        lines.add(json.writeValueAsString(payload));

        Path tmp = Files.createTempFile(logDir, "swap", null);
        Files.write(tmp, lines, UTF_8, TRUNCATE_EXISTING);
        Files.move(tmp, file, ATOMIC_MOVE, REPLACE_EXISTING);
    }
}
