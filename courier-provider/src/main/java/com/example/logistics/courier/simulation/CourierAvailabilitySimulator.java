package com.example.logistics.courier.simulation;

import com.example.logistics.courier.config.CourierSimulationProperties;
import com.example.logistics.courier.entity.CourierEntity;
import com.example.logistics.courier.repository.CourierRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CourierAvailabilitySimulator {

    private static final Logger log = LoggerFactory.getLogger(CourierAvailabilitySimulator.class);

    private final CourierRepository couriers;
    private final CourierSimulationProperties properties;

    public CourierAvailabilitySimulator(CourierRepository couriers, CourierSimulationProperties properties) {
        this.couriers = couriers;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${courier.simulation.update-interval}")
    @Transactional
    public void updateAvailability() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Instant now = Instant.now();
        for (CourierEntity courier : couriers.findAll()) {
            boolean available = random.nextDouble() < properties.availabilityProbability();
            long maxGrams = courier.getMaxWeight().movePointRight(3).longValueExact();
            long loadGrams = random.nextLong(available ? maxGrams : maxGrams + 1);
            courier.updateAvailability(available, BigDecimal.valueOf(loadGrams, 3), now);
        }
        log.debug("Courier availability updated");
    }
}
