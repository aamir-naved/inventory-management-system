package com.inventory.common.idempotency;

import java.net.URI;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.common.api.ConflictException;
import com.inventory.common.tenant.TenantContext;

@Service
public class IdempotencyService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public <T> ResponseEntity<T> execute(
        String idempotencyKey,
        String httpMethod,
        String requestPath,
        Object requestBody,
        Class<T> responseType,
        Supplier<T> action,
        Function<T, URI> locationFactory
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            T body = action.get();
            URI location = locationFactory.apply(body);
            return ResponseEntity.created(location).body(body);
        }

        String key = idempotencyKey.trim();
        if (key.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must be 128 characters or fewer");
        }

        UUID businessId = TenantContext.getBusinessId()
            .orElseThrow(() -> new IllegalStateException("Business context is required"));
        String requestHash = hashRequest(requestBody);

        var existing = repository.findByBusinessIdAndIdempotencyKey(businessId, key);
        if (existing.isPresent()) {
            return replay(existing.get(), requestHash, responseType);
        }

        IdempotencyKeyEntity claim = new IdempotencyKeyEntity();
        claim.setBusinessId(businessId);
        claim.setIdempotencyKey(key);
        claim.setHttpMethod(httpMethod);
        claim.setRequestPath(requestPath);
        claim.setRequestHash(requestHash);
        claim.setStatus(STATUS_PENDING);

        try {
            repository.saveAndFlush(claim);
        } catch (DataIntegrityViolationException exception) {
            IdempotencyKeyEntity raced = repository
                .findByBusinessIdAndIdempotencyKey(businessId, key)
                .orElseThrow(() -> exception);
            return replay(raced, requestHash, responseType);
        }

        T body = action.get();
        URI location = locationFactory.apply(body);
        claim.setStatus(STATUS_COMPLETED);
        claim.setStatusCode(201);
        claim.setLocationHeader(location == null ? null : location.toString());
        try {
            claim.setResponseBody(objectMapper.writeValueAsString(body));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to store idempotent response", exception);
        }
        repository.save(claim);

        return ResponseEntity.created(location).body(body);
    }

    private <T> ResponseEntity<T> replay(
        IdempotencyKeyEntity stored,
        String requestHash,
        Class<T> responseType
    ) {
        if (!stored.getRequestHash().equals(requestHash)) {
            throw new ConflictException(
                "Idempotency-Key was reused with a different request body"
            );
        }
        if (STATUS_PENDING.equals(stored.getStatus())) {
            throw new ConflictException(
                "A request with this Idempotency-Key is already in progress"
            );
        }
        try {
            T body = objectMapper.readValue(stored.getResponseBody(), responseType);
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(
                stored.getStatusCode() == null ? 201 : stored.getStatusCode()
            );
            if (stored.getLocationHeader() != null && !stored.getLocationHeader().isBlank()) {
                builder.header("Location", stored.getLocationHeader());
            }
            return builder.body(body);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to replay stored idempotent response", exception);
        }
    }

    private String hashRequest(Object requestBody) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(requestBody);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash idempotent request", exception);
        }
    }
}
