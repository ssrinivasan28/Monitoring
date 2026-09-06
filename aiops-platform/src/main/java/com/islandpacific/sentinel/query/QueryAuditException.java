package com.islandpacific.sentinel.query;

public class QueryAuditException extends RuntimeException {
    public QueryAuditException(String message, Throwable cause) {
        super(message, cause);
    }
}
