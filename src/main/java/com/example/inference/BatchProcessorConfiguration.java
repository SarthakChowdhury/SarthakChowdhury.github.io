package com.example.inference;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class BatchProcessorConfiguration extends Configuration {
    @Min(1)
    @Max(256)
    private int maxConcurrentWorkers = 10;

    @Min(1)
    private int queueCapacity = 1000;

    @Min(1)
    private int maxRetries = 5;

    @Min(1)
    private int retryInitialDelayMs = 500;

    @Min(0)
    @Max(1)
    private double retryJitterFactor = 0.2;

    @Min(0)
    @Max(1)
    private double rateLimitChance = 0.15;

    @Min(0)
    private int mockLatencyMinMs = 100;

    @Min(0)
    private int mockLatencyMaxMs = 300;

    @NotEmpty
    private String dbUrl = "jdbc:h2:file:./.h2/batch-db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";

    @NotNull
    private String dbUser = "sa";

    @NotNull
    private String dbPassword = "";

    @JsonProperty
    public int getMaxConcurrentWorkers() {
        return maxConcurrentWorkers;
    }

    @JsonProperty
    public void setMaxConcurrentWorkers(int maxConcurrentWorkers) {
        this.maxConcurrentWorkers = maxConcurrentWorkers;
    }

    @JsonProperty
    public int getQueueCapacity() {
        return queueCapacity;
    }

    @JsonProperty
    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    @JsonProperty
    public int getMaxRetries() {
        return maxRetries;
    }

    @JsonProperty
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @JsonProperty
    public int getRetryInitialDelayMs() {
        return retryInitialDelayMs;
    }

    @JsonProperty
    public void setRetryInitialDelayMs(int retryInitialDelayMs) {
        this.retryInitialDelayMs = retryInitialDelayMs;
    }

    @JsonProperty
    public double getRetryJitterFactor() {
        return retryJitterFactor;
    }

    @JsonProperty
    public void setRetryJitterFactor(double retryJitterFactor) {
        this.retryJitterFactor = retryJitterFactor;
    }

    @JsonProperty
    public double getRateLimitChance() {
        return rateLimitChance;
    }

    @JsonProperty
    public void setRateLimitChance(double rateLimitChance) {
        this.rateLimitChance = rateLimitChance;
    }

    @JsonProperty
    public int getMockLatencyMinMs() {
        return mockLatencyMinMs;
    }

    @JsonProperty
    public void setMockLatencyMinMs(int mockLatencyMinMs) {
        this.mockLatencyMinMs = mockLatencyMinMs;
    }

    @JsonProperty
    public int getMockLatencyMaxMs() {
        return mockLatencyMaxMs;
    }

    @JsonProperty
    public void setMockLatencyMaxMs(int mockLatencyMaxMs) {
        this.mockLatencyMaxMs = mockLatencyMaxMs;
    }

    @JsonProperty
    public String getDbUrl() {
        return dbUrl;
    }

    @JsonProperty
    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    @JsonProperty
    public String getDbUser() {
        return dbUser;
    }

    @JsonProperty
    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    @JsonProperty
    public String getDbPassword() {
        return dbPassword;
    }

    @JsonProperty
    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }
}
