package com.example.inference;

import io.dropwizard.core.Configuration;

public class BatchProcessorConfiguration extends Configuration {
    private int maxConcurrentWorkers = 4;
    private int maxRetries = 3;
    private int retryInitialDelayMs = 500;
    private double retryJitterFactor = 0.2;
    private String dbUrl = "jdbc:h2:mem:batch;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
    private String dbUser = "sa";
    private String dbPassword = "";

    public int getMaxConcurrentWorkers() {
        return maxConcurrentWorkers;
    }

    public void setMaxConcurrentWorkers(int maxConcurrentWorkers) {
        this.maxConcurrentWorkers = maxConcurrentWorkers;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getRetryInitialDelayMs() {
        return retryInitialDelayMs;
    }

    public void setRetryInitialDelayMs(int retryInitialDelayMs) {
        this.retryInitialDelayMs = retryInitialDelayMs;
    }

    public double getRetryJitterFactor() {
        return retryJitterFactor;
    }

    public void setRetryJitterFactor(double retryJitterFactor) {
        this.retryJitterFactor = retryJitterFactor;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }
}
