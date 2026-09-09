package com.islandpacific.sentinel.integration.teams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 2.3 - the subset of a Microsoft Teams Outgoing Webhook activity payload ChatOps actually reads:
 * the message text and the sender's Azure AD object id (Bot Framework {@code ChannelAccount.aadObjectId},
 * populated by Teams for every channel message). Every other field on the real payload (conversation,
 * channelData, entities, timestamps, ...) is ignored, not modeled.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TeamsChatMessage {

    private String text;
    private From from;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public From getFrom() { return from; }
    public void setFrom(From from) { this.from = from; }

    /** Text with any leading @mention of the bot stripped, since Teams includes it verbatim in {@code text}. */
    public String questionText() {
        if (text == null) {
            return "";
        }
        return text.replaceFirst("^\\s*<at>[^<]*</at>\\s*", "").trim();
    }

    public String aadObjectId() {
        return from != null ? from.getAadObjectId() : null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class From {
        private String id;
        private String name;

        @JsonProperty("aadObjectId")
        private String aadObjectId;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getAadObjectId() { return aadObjectId; }
        public void setAadObjectId(String aadObjectId) { this.aadObjectId = aadObjectId; }
    }
}
