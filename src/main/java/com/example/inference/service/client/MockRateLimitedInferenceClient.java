package com.example.inference.service.client;

import com.example.inference.model.InferenceResult;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class MockRateLimitedInferenceClient {
    private final int maxRequestsPerWindow;
    private final long windowMs;
    private Instant windowStart = Instant.now();
    private int windowCount = 0;

    public MockRateLimitedInferenceClient(int maxRequestsPerWindow, long windowMs) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowMs = windowMs;
    }

    public synchronized InferenceResult infer(String prompt) {
        while (true) {
            Instant now = Instant.now();
            long elapsed = Duration.between(windowStart, now).toMillis();
            if (elapsed >= windowMs) {
                windowStart = now;
                windowCount = 0;
            }

            if (windowCount < maxRequestsPerWindow) {
                windowCount++;
                try {
                    TimeUnit.MILLISECONDS.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                return new InferenceResult(prompt, "mock-response:" + prompt.toUpperCase());
            }

            try {
                wait(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
}
