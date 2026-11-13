package com.example.logistics.management.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @GetMapping("/api/v1/status")
    public Map<String, String> status() {
        // по ТЗ. хотя есть и actuator
        return Map.of("service", "shipment-management", "status", "UP");
    }
}
