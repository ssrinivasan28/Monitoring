package com.islandpacific.monitoring.ibmqsysoprmonitoring;

import java.sql.Timestamp;


public class MessageInfo {
    private final String messageId;
    private final String messageText;
    private final Timestamp messageTimestamp;
    private final String fromJob;
    private final String fromUser;


    public MessageInfo(String messageId, String messageText, Timestamp messageTimestamp, String fromJob, String fromUser) {
        this.messageId = messageId;
        this.messageText = messageText;
        this.messageTimestamp = messageTimestamp;
        this.fromJob = fromJob;
        this.fromUser = fromUser;
    }

    // --- Getters for message properties ---
    public String getMessageId() {
        return messageId;
    }

    public String getMessageText() {
        return messageText;
    }

    public Timestamp getMessageTimestamp() {
        return messageTimestamp;
    }

    public String getFromJob() {
        return fromJob;
    }

    public String getFromUser() {
        return fromUser;
    }


    public String getCompositeKey() {
        return messageId + "|" + fromJob;
    }

    @Override
    public String toString() {
        return "MessageInfo{" +
               "messageId='" + messageId + '\'' +
               ", messageText='" + messageText + '\'' +
               ", messageTimestamp=" + messageTimestamp +
               ", fromJob='" + fromJob + '\'' +
               ", fromUser='" + fromUser + '\'' +
               '}';
    }
}
