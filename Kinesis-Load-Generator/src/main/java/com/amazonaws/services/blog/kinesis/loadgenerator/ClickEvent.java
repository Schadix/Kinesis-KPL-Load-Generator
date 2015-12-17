package com.amazonaws.services.blog.kinesis.loadgenerator;

public class ClickEvent {
    private String sessionId;
    private String payload;

    public ClickEvent(String sessionId, String payload) {
        this.sessionId = sessionId;
        this.payload = payload;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getPayload() {
        return payload;
    }
}