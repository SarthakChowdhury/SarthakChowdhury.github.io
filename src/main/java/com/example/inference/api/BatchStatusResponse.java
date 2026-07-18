package com.example.inference.api;

public class BatchStatusResponse {
    private final String batchId;
    private final String status;
    private final int totalPrompts;
    private final int completed;
    private final int failed;

    public BatchStatusResponse(String batchId, String status, int totalPrompts, int completed, int failed) {
        this.batchId = batchId;
        this.status = status;
        this.totalPrompts = totalPrompts;
        this.completed = completed;
        this.failed = failed;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getStatus() {
        return status;
    }

    public int getTotalPrompts() {
        return totalPrompts;
    }

    public int getCompleted() {
        return completed;
    }

    public int getFailed() {
        return failed;
    }

    public String status() {
        return status;
    }

    public int completed() {
        return completed;
    }

    public int failed() {
        return failed;
    }
}
