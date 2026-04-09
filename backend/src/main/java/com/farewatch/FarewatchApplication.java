package com.farewatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FarewatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(FarewatchApplication.class, args);
    }
}
