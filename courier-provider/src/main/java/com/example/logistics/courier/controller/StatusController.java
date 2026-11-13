package com.example.logistics.courier.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @GetMapping("/api/v1/status")
    public Map<String, String> status() {
        return Map.of("service", "courier-provider", "status", "UP");
    }
}
