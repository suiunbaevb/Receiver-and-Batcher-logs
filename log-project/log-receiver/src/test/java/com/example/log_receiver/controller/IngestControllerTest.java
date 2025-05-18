// log-receiver/src/test/java/com/example/log_receiver/controller/IngestControllerTest.java
package com.example.log_receiver.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IngestControllerTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        // Указываем log-dir на временную папку
        registry.add("log-dir", () -> tempDir.toString());
        // Сервис подберёт свободный порт
        registry.add("server.port", () -> "0");
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void whenPostValidXml_thenCreatesLogFileWithCounterAndJson() throws Exception {
        String xml =
                "<Data>\n" +
                        "  <Type>Information</Type>\n" +
                        "  <Value>Hello</Value>\n" +
                        "</Data>";

        // Отправляем POST / с XML
        mockMvc.perform(post("/")
                        .contentType(APPLICATION_XML)
                        .content(xml))
                .andExpect(status().isOk());

        // Проверяем, что файл появился
        String date = LocalDate.now().toString();
        Path file = tempDir.resolve("Information-" + date + ".log");
        assertThat(Files.exists(file)).isTrue();

        // Считаем строки
        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).trim()).isEqualTo("1");            // счётчик
        assertThat(lines.get(1)).contains("\"Data\"").contains("\"Type\":\"Information\"");  // JSON
    }
}
