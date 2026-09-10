package com.inventory.common.api;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
