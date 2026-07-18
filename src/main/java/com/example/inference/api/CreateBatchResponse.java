package com.example.inference.api;

public class CreateBatchResponse {
    private final String batchId;
    private final String status;
    private final String message;

    public CreateBatchResponse(String batchId, String status, String message) {
        this.batchId = batchId;
        this.status = status;
        this.message = message;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
