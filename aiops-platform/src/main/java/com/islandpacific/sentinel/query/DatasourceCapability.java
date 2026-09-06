package com.islandpacific.sentinel.query;

public enum DatasourceCapability {
    PROMETHEUS,
    THANOS,
    LOKI,
    SUPPORTS_TENANT_LABEL,
    SUPPORTS_QUERY_RANGE,
    SUPPORTS_PARTIAL_RESPONSE
}
