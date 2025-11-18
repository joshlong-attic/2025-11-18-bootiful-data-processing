package com.example.integration;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.dsl.Files;

import java.io.File;

@SpringBootApplication
public class IntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationApplication.class, args);
    }

    @Bean
    IntegrationFlow fileIntegrationFlow(@Value("file://${user.home}/Desktop/in") File directory) {
        var log = LoggerFactory.getLogger(getClass());
        return IntegrationFlow
                .from(Files.inboundAdapter(directory).autoCreateDirectory(true))
                .handle((GenericHandler<File>) (payload, headers) -> {
                    log.info(payload.getAbsolutePath());
                    headers.forEach((k, v) -> log.info(k + ": " + v));
                    return null;
                })
                .get();
    }
}
