package com.inventory.platform.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inventory.common.api.PagedResponse;
import com.inventory.platform.dto.PlatformCreateShopRequest;
import com.inventory.platform.dto.PlatformCreateShopResponse;
import com.inventory.platform.dto.PlatformPasswordResetResponse;
import com.inventory.platform.dto.PlatformSettingsResponse;
import com.inventory.platform.dto.PlatformShopDetailResponse;
import com.inventory.platform.dto.PlatformShopSummaryResponse;
import com.inventory.platform.dto.PlatformShopUpdateRequest;
import com.inventory.platform.dto.PlatformStatsResponse;
import com.inventory.platform.dto.PlatformUserResponse;
import com.inventory.platform.dto.PlatformUserUpdateRequest;
import com.inventory.platform.service.PlatformService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/platform")
public class PlatformController {

    private final PlatformService platformService;

    public PlatformController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/stats")
    public PlatformStatsResponse stats() {
        return platformService.stats();
    }

    @GetMapping("/settings")
    public PlatformSettingsResponse settings() {
        return platformService.settings();
    }

    @GetMapping("/shops")
    public PagedResponse<PlatformShopSummaryResponse> shops(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return platformService.listShops(status, search, page, size);
    }

    @PostMapping("/shops")
    public ResponseEntity<PlatformCreateShopResponse> createShop(@Valid @RequestBody PlatformCreateShopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createShop(request));
    }

    @GetMapping("/shops/{id}")
    public PlatformShopDetailResponse shop(@PathVariable UUID id) {
        return platformService.getShop(id);
    }

    @PatchMapping("/shops/{id}")
    public PlatformShopDetailResponse updateShop(
        @PathVariable UUID id,
        @Valid @RequestBody PlatformShopUpdateRequest request
    ) {
        return platformService.updateShop(id, request);
    }

    @PostMapping("/shops/{id}/reset-owner")
    public PlatformPasswordResetResponse resetOwner(@PathVariable UUID id) {
        return platformService.resetOwnerPassword(id);
    }

    @GetMapping("/users")
    public PagedResponse<PlatformUserResponse> users(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return platformService.listUsers(search, page, size);
    }

    @PatchMapping("/users/{id}")
    public PlatformUserResponse updateUser(
        @PathVariable UUID id,
        @Valid @RequestBody PlatformUserUpdateRequest request
    ) {
        return platformService.updateUser(id, request);
    }
}
