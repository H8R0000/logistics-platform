package com.example.logistics.processing.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Создаёт управляемый Spring пул рабочих потоков и включает периодическую передачу задач из БД. */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({ProcessingProperties.class, CourierClientProperties.class, DeliverySimulationProperties.class})
public class ProcessingConfiguration {

    @Bean
    ThreadPoolTaskExecutor processingExecutor(ProcessingProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.executor().corePoolSize());
        executor.setMaxPoolSize(properties.executor().maxPoolSize());
        executor.setQueueCapacity(properties.executor().queueCapacity());
        executor.setThreadNamePrefix("shipment-worker-");
        executor.setRejectedExecutionHandler(switch (properties.executor().rejectionPolicy()) {
            case ABORT -> new ThreadPoolExecutor.AbortPolicy();
            case CALLER_RUNS -> new ThreadPoolExecutor.CallerRunsPolicy();
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        return executor;
    }
}
