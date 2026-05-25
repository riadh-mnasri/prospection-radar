package com.radar.prospection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProspectionRadarApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProspectionRadarApplication.class, args);
    }
}
