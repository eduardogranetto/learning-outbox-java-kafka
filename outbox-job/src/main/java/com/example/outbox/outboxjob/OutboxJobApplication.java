package com.example.outbox.outboxjob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OutboxJobApplication {
    public static void main(String[] args) {
        SpringApplication.run(OutboxJobApplication.class, args);
    }
}
