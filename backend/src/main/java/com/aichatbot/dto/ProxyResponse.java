package com.aichatbot.dto;

import java.util.Map;

public class ProxyResponse {
    private int status;
    private String statusText;
    private Map<String, String> headers;
    private String body;

    public ProxyResponse(int status, String statusText, Map<String, String> headers, String body) {
        this.status = status;
        this.statusText = statusText;
        this.headers = headers;
        this.body = body;
    }

    public int getStatus() { return status; }
    public String getStatusText() { return statusText; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }
}
