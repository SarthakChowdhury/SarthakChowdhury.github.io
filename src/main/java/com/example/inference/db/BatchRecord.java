package com.example.inference.db;

public class BatchRecord {
    private String batchId;
    private String status;
    private int totalPrompts;
    private int completed;
    private int failed;

    public String getbatchId() {
        return batchId;
    }

    public void setbatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getstatus() {
        return status;
    }

    public void setstatus(String status) {
        this.status = status;
    }

    public int gettotalPrompts() {
        return totalPrompts;
    }

    public void settotalPrompts(int totalPrompts) {
        this.totalPrompts = totalPrompts;
    }

    public int getcompleted() {
        return completed;
    }

    public void setcompleted(int completed) {
        this.completed = completed;
    }

    public int getfailed() {
        return failed;
    }

    public void setfailed(int failed) {
        this.failed = failed;
    }

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
