package com.example.log_receiver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class ThreadingConfig {

    @Bean
    public Executor customExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor(); // Loom
    }
}
