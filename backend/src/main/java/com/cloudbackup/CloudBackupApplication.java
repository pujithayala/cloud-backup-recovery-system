package com.cloudbackup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CloudBackupApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudBackupApplication.class, args);
    }
}

