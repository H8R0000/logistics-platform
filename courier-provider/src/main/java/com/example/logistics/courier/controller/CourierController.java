package com.example.logistics.courier.controller;

import com.example.logistics.courier.dto.CourierResponse;
import com.example.logistics.courier.dto.CouriersResponse;
import com.example.logistics.courier.service.CourierService;
import com.example.logistics.courier.simulation.CourierResponseSimulator;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/couriers")
public class CourierController {

    private final CourierService service;
    private final CourierResponseSimulator responseSimulator;

    public CourierController(CourierService service, CourierResponseSimulator responseSimulator) {
        this.service = service;
        this.responseSimulator = responseSimulator;
    }

    @GetMapping
    public CouriersResponse list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public CourierResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/available")
    public CouriersResponse available() {
        responseSimulator.simulate();
        return service.available();
    }
}
