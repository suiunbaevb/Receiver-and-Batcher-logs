package com.example.logbatcher.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class OffsetRepository {

    private final Path offsetFile;   // logs/batch.offset  (путь из конфигурации)

    /** dir — значение свойства log-dir (application.yml, переменная окружения). */
    public OffsetRepository(@Value("${log-dir:logs}") String dir) {
        this.offsetFile = Paths.get(dir, "batch.offset").toAbsolutePath();
    }

    /** Считываем, сколько строк уже упаковано. */
    public long load() {
        try {
            return Files.exists(offsetFile)
                    ? Long.parseLong(Files.readString(offsetFile).trim())
                    : 0L;
        } catch (IOException e) {
            return 0L;
        }
    }

    /** Сохраняем новый офсет. */
    public void save(long value) {
        try {
            Files.createDirectories(offsetFile.getParent());
            Files.writeString(offsetFile, Long.toString(value));
        } catch (IOException ignored) {
        }
    }
}
