package com.islandpacific.sentinel.service.incident;

import java.util.List;

/** One page of ranked incidents plus the paging metadata the controller surfaces as response headers. */
public class IncidentListResult {

    private final List<IncidentSummaryDto> content;
    private final int totalElements;
    private final int page;
    private final int size;

    public IncidentListResult(List<IncidentSummaryDto> content, int totalElements, int page, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.page = page;
        this.size = size;
    }

    public List<IncidentSummaryDto> getContent() { return content; }
    public int getTotalElements() { return totalElements; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public int getTotalPages() { return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size); }
}
