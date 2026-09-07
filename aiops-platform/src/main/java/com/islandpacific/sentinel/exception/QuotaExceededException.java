package com.islandpacific.sentinel.exception;

/**
 * Thrown when a tenant exceeds daily or monthly token or cost quotas.
 */
public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String message) {
        super(message);
    }
}
