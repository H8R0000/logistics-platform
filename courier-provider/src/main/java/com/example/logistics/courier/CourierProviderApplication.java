package com.example.logistics.courier;

import com.example.logistics.courier.config.CourierSimulationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(CourierSimulationProperties.class)
public class CourierProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourierProviderApplication.class, args);
    }
}
