package com.example.logistics.courier.simulation;

import com.example.logistics.courier.config.CourierSimulationProperties;
import com.example.logistics.courier.exception.CourierUnavailableException;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class CourierResponseSimulator {

    private final CourierSimulationProperties properties;

    public CourierResponseSimulator(CourierSimulationProperties properties) {
        this.properties = properties;
    }

    public void simulate() {
        try {
            Thread.sleep(properties.responseDelay());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CourierUnavailableException("Courier response interrupted");
        }
        if (ThreadLocalRandom.current().nextDouble() < properties.errorProbability()) {
            throw new CourierUnavailableException("Simulated courier provider failure");
        }
    }
}
