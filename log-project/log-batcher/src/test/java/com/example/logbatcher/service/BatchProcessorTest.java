// log-batcher/src/test/java/com/example/logbatcher/service/BatchProcessorTest.java
package com.example.logbatcher.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BatchProcessorTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        // Указываем общий каталог логов
        registry.add("log-dir", () -> tempDir.toString());
        // Для теста поставим маленький batch.size
        registry.add("batch.size", () -> "10");
    }

    @Autowired
    BatchProcessor batchProcessor;

    @Autowired
    OffsetRepository offsets;

    private Path rawLog;

    @BeforeEach
    void setup() throws Exception {
        // Создаём файл Information-YYYY-MM-DD.log с 150 строками
        String date = LocalDate.now().toString();
        rawLog = tempDir.resolve("Information-" + date + ".log");

        // Первая строка — текущий офсет (0)
        List<String> lines = new ArrayList<>();
        lines.add("0");
        // Добавляем 150 записей
        for (int i = 1; i <= 150; i++) {
            lines.add("{\"Data\":{\"Type\":\"Information\",\"idx\":" + i + "}}");
        }
        Files.write(rawLog, lines);
    }

    @Test
    void whenRunBatchProcessor_thenCreatesBatchesAndUpdatesOffset() throws Exception {
        // Запускаем батчер
        batchProcessor.run();

        // Офсет должен стать 150
        long offset = offsets.load();
        assertThat(offset).isEqualTo(150);

        String base = rawLog.getFileName().toString().replace(".log", "");
        // Должны быть 15 файлов по 10 строк
        for (int i = 1; i <= 15; i++) {
            String batchName = String.format("%s-%04d.log", base, i);
            Path batchFile = tempDir.resolve(batchName);
            assertThat(Files.exists(batchFile)).isTrue();
            List<String> batchLines = Files.readAllLines(batchFile);
            assertThat(batchLines).hasSize(10);
        }
    }
}
