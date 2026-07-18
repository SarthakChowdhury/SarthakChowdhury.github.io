package com.example.inference.core;

import com.example.inference.model.InferenceResult;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MockInferenceClient {
    private final double rateLimitChance;
    private final long latencyMs;
    private final int seed;
    private final Random random;

    public MockInferenceClient(double rateLimitChance, long latencyMs, int seed) {
        this.rateLimitChance = rateLimitChance;
        this.latencyMs = latencyMs;
        this.seed = seed;
        this.random = new Random(seed);
    }

    public InferenceResult infer(String prompt) throws Exception {
        if (latencyMs > 0) {
            TimeUnit.MILLISECONDS.sleep(latencyMs);
        }
        if (random.nextDouble() < rateLimitChance) {
            throw new InferenceRateLimitException("simulated 429");
        }
        return new InferenceResult(prompt, "mock-response:" + prompt.toUpperCase());
    }
}
