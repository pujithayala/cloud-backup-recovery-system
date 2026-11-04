package com.cloudbackup.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${app.storage.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${app.storage.access-key:minioadmin}")
    private String accessKey;

    @Value("${app.storage.secret-key:minioadmin}")
    private String secretKey;

    @Value("${app.storage.region:us-east-1}")
    private String region;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();
    }
}

