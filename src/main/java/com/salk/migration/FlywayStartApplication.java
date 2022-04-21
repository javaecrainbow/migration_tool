package com.salk.migration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.salk.**")
public class FlywayStartApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlywayStartApplication.class, args);
    }

}
