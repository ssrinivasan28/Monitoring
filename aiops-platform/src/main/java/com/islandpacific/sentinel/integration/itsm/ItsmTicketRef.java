package com.islandpacific.sentinel.integration.itsm;

/** A freshly-created ITSM ticket reference, returned by {@link ItsmAdapter#createTicket}. */
public final class ItsmTicketRef {

    private final String externalId;
    private final String externalUrl;
    private final String itsmState;

    public ItsmTicketRef(String externalId, String externalUrl, String itsmState) {
        this.externalId = externalId;
        this.externalUrl = externalUrl;
        this.itsmState = itsmState;
    }

    public String getExternalId() { return externalId; }
    public String getExternalUrl() { return externalUrl; }
    public String getItsmState() { return itsmState; }
}
