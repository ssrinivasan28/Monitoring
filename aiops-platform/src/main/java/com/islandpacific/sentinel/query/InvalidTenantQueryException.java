package com.islandpacific.sentinel.query;

public class InvalidTenantQueryException extends RuntimeException {
    public InvalidTenantQueryException(String message) {
        super(message);
    }
}
