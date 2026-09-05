package com.inventory.business.controller;

import com.inventory.business.dto.BusinessRequest;
import com.inventory.business.dto.BusinessResponse;
import com.inventory.business.dto.QuickStartRequest;
import com.inventory.business.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @PostMapping
    public ResponseEntity<BusinessResponse> create(@Valid @RequestBody BusinessRequest request) {
        BusinessResponse response = businessService.create(request);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/quick-start")
    public ResponseEntity<BusinessResponse> quickStart(@Valid @RequestBody QuickStartRequest request) {
        BusinessResponse response = businessService.quickStart(request);
        URI location = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/businesses/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public BusinessResponse getById(@PathVariable UUID id) {
        return businessService.getById(id);
    }

    @PatchMapping("/{id}")
    public BusinessResponse update(@PathVariable UUID id, @Valid @RequestBody BusinessRequest request) {
        return businessService.update(id, request);
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BusinessResponse uploadLogo(
        @PathVariable UUID id,
        @RequestParam("file") MultipartFile file
    ) throws java.io.IOException {
        return businessService.uploadLogo(id, file.getBytes(), file.getContentType());
    }

    @GetMapping("/{id}/logo")
    public ResponseEntity<byte[]> logo(@PathVariable UUID id) {
        byte[] bytes = businessService.logoBytes(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, businessService.logoContentType(id))
            .body(bytes);
    }

    @DeleteMapping("/{id}/logo")
    public ResponseEntity<Void> removeLogo(@PathVariable UUID id) {
        businessService.removeLogo(id);
        return ResponseEntity.noContent().build();
    }
}
