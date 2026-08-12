package com.quini6.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Quini6AnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(Quini6AnalyticsApplication.class, args);
    }
}
