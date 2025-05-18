package com.example.logbatcher.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class BatchProcessor implements CommandLineRunner {

    /** Каталог логов берём из конфигурации: log-dir: ../logs */
    @Value("${log-dir:logs}")
    private String logDirProp;

    /** Размер пачки также выносим в конфиг: batch.size: 100 */
    @Value("${batch.size:100}")
    private int BATCH_SIZE;

    private Path LOG_DIR;              // инициализируем в run()
    private final OffsetRepository offsets;

    @Override
    public void run(String... args) throws Exception {

        LOG_DIR = Paths.get(logDirProp).toAbsolutePath();
        if (!Files.isDirectory(LOG_DIR)) {
            System.out.println("No logs directory yet – nothing to batch.");
            return;
        }

        long processed = offsets.load();
        long seen = 0;

        try (Stream<Path> paths = Files.list(LOG_DIR)) {
            List<Path> sources = paths
                    .filter(p -> p.getFileName().toString()
                            .matches("^[A-Za-z]+-\\d{4}-\\d{2}-\\d{2}\\.log$"))
                    .sorted()
                    .collect(Collectors.toList());

            for (Path src : sources) {
                try (Stream<String> lines = Files.lines(src).skip(1)) { // пропускаем счётчик
                    Iterator<String> it = lines.iterator();
                    while (it.hasNext()) {
                        String line = it.next();
                        seen++;
                        if (seen <= processed) continue;               // уже упаковано

                        List<String> batch = new ArrayList<>(BATCH_SIZE);
                        batch.add(line);
                        while (it.hasNext() && batch.size() < BATCH_SIZE) {
                            batch.add(it.next());
                            seen++;
                        }

                        writeBatchFile(src, batch, seen);
                        offsets.save(seen);
                    }
                }
            }
        }
        System.out.println("Batching finished. Total processed: " + seen);
    }

    private void writeBatchFile(Path sourceFile,
                                List<String> records,
                                long processedSoFar) throws IOException {

        String base = sourceFile.getFileName().toString().replace(".log", "");
        long idx = (processedSoFar - 1) / BATCH_SIZE + 1;              // 0001, 0002, …
        String targetName = String.format("%s-%04d.log", base, idx);
        Path target = LOG_DIR.resolve(targetName);

        Files.write(target, records, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        System.out.printf(" → wrote %d record(s) to %s%n", records.size(), targetName);
    }
}
