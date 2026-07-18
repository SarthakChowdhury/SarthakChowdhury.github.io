package com.example.inference.db;

public class BatchRecord {
    private String batchId;
    private String status;
    private int totalPrompts;
    private int completed;
    private int failed;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalPrompts() {
        return totalPrompts;
    }

    public void setTotalPrompts(int totalPrompts) {
        this.totalPrompts = totalPrompts;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }
}
