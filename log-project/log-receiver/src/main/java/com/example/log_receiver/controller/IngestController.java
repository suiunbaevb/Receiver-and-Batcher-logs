package com.example.log_receiver.controller;

import com.example.log_receiver.service.JsonWriterService;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
public class IngestController {

    private final JsonWriterService writer;
    private final XmlMapper xml = new XmlMapper(); // thread-safe (Jackson ≥ 2.17)

    @Autowired
    public IngestController(JsonWriterService writer) {
        this.writer = writer;
    }

    @PostMapping("/")
    public ResponseEntity<String> ingest(@RequestBody byte[] body) throws IOException {
        if (body.length == 0) {
            return ResponseEntity.badRequest().body("Empty body");
        }

        /* 1) XML → Map */
        Map<String, Object> root = xml.readValue(body, Map.class);

        /* 2) Определяем, где находится блок Data */
        Map<?, ?> data;
        if (root.containsKey("Data")) {                 // <Root><Data>…</Data></Root>
            data = (Map<?, ?>) root.get("Data");
        } else {                                        // <Data>…</Data>
            data = root;
            root = Map.of("Data", root);                // оборачиваем, чтобы был ключ "Data"
        }

        if (!data.containsKey("Type")) {
            return ResponseEntity.badRequest().body("/Data/Type missing");
        }

        /* 3) Записываем */
        String type = data.get("Type").toString();
        writer.appendRecord(type, root);                // root всегда содержит "Data"
        return ResponseEntity.ok("OK");
    }
}
