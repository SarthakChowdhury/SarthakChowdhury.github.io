package com.example.inference.core;

import com.example.inference.model.InferenceResult;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class MockInferenceClient {
    private final double rateLimitChance;
    private final int latencyMinMs;
    private final int latencyMaxMs;

    public MockInferenceClient(double rateLimitChance, int latencyMinMs, int latencyMaxMs) {
        this.rateLimitChance = rateLimitChance;
        this.latencyMinMs = Math.max(0, latencyMinMs);
        this.latencyMaxMs = Math.max(this.latencyMinMs, latencyMaxMs);
    }

    /** Convenience constructor for fixed-latency tests. */
    public MockInferenceClient(double rateLimitChance, long latencyMs, int ignoredSeed) {
        this(rateLimitChance, (int) latencyMs, (int) latencyMs);
    }

    public InferenceResult infer(String prompt) throws Exception {
        int span = latencyMaxMs - latencyMinMs;
        int delay = latencyMinMs + (span == 0 ? 0 : ThreadLocalRandom.current().nextInt(span + 1));
        if (delay > 0) {
            TimeUnit.MILLISECONDS.sleep(delay);
        }
        if (ThreadLocalRandom.current().nextDouble() < rateLimitChance) {
            throw new InferenceRateLimitException("simulated 429");
        }
        return new InferenceResult(prompt, "mock-response:" + prompt.toUpperCase());
    }
}
