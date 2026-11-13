package com.example.logistics.management.controller;

import com.example.logistics.management.dto.CreateShipmentRequest;
import com.example.logistics.management.dto.PageResponse;
import com.example.logistics.management.dto.ShipmentAcceptedResponse;
import com.example.logistics.management.dto.ShipmentEventResponse;
import com.example.logistics.management.dto.ShipmentResponse;
import com.example.logistics.management.entity.DeliveryType;
import com.example.logistics.management.entity.ShipmentStatus;
import com.example.logistics.management.security.CurrentUser;
import com.example.logistics.management.service.ShipmentSearchFilter;
import com.example.logistics.management.service.ShipmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private final ShipmentService service;

    public ShipmentController(ShipmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ShipmentAcceptedResponse> create(
        @Valid @RequestBody CreateShipmentRequest request, JwtAuthenticationToken authentication) {
        ShipmentAcceptedResponse response = service.create(request, CurrentUser.from(authentication));
        return ResponseEntity.accepted()
            .location(URI.create("/api/v1/shipments/" + response.id()))
            .body(response);
    }

    @GetMapping("/{id}")
    public ShipmentResponse get(@PathVariable UUID id, JwtAuthenticationToken authentication) {
        return service.get(id, CurrentUser.from(authentication));
    }

    @GetMapping
    public PageResponse<ShipmentResponse> list(
        @RequestParam(required = false) ShipmentStatus status,
        @RequestParam(required = false) DeliveryType deliveryType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "createdAt,desc") @NotNull String sort,
        JwtAuthenticationToken authentication) {
        ShipmentSearchFilter filter = new ShipmentSearchFilter(status, deliveryType, createdFrom, createdTo, page, size, sort);
        return service.list(filter, CurrentUser.from(authentication));
    }

    @GetMapping("/{id}/events")
    public List<ShipmentEventResponse> history(@PathVariable UUID id, JwtAuthenticationToken authentication) {
        return service.history(id, CurrentUser.from(authentication));
    }
}
