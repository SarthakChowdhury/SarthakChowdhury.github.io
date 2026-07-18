package com.example.inference.service;

import com.example.inference.model.InferenceResult;
import com.example.inference.service.client.MockRateLimitedInferenceClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class InferenceService {
    private final ExecutorService executor;
    private final MockRateLimitedInferenceClient inferenceClient;

    public InferenceService() {
        this(Executors.newFixedThreadPool(8), new MockRateLimitedInferenceClient(4, 250));
    }

    public InferenceService(ExecutorService executor, MockRateLimitedInferenceClient inferenceClient) {
        this.executor = executor;
        this.inferenceClient = inferenceClient;
    }

    public List<InferenceResult> runBatch(List<String> prompts) throws InterruptedException, ExecutionException {
        List<Future<InferenceResult>> futures = new ArrayList<>();
        for (String prompt : prompts) {
            futures.add(executor.submit(() -> inferenceClient.infer(prompt)));
        }

        List<InferenceResult> results = new ArrayList<>();
        for (Future<InferenceResult> future : futures) {
            results.add(future.get());
        }

        return results;
    }
}
