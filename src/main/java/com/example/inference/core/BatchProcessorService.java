package com.example.inference.core;

import com.example.inference.api.BatchStatusResponse;
import com.example.inference.db.BatchDao;
import com.example.inference.db.BatchRecord;
import com.example.inference.model.InferenceResult;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BatchProcessorService {
    private final BatchDao batchDao;
    private final MockInferenceClient inferenceClient;
    private final ExecutorService workerPool;
    private final int maxRetries;
    private final Duration initialBackoff;
    private final double jitterFactor;

    public BatchProcessorService(BatchDao batchDao, MockInferenceClient inferenceClient, int workers, Duration initialBackoff, int maxRetries, double jitterFactor) {
        this.batchDao = batchDao;
        this.inferenceClient = inferenceClient;
        this.workerPool = Executors.newFixedThreadPool(workers);
        this.maxRetries = maxRetries;
        this.initialBackoff = initialBackoff;
        this.jitterFactor = jitterFactor;
    }

    public String createBatch(List<String> prompts) {
        String batchId = batchDao.createBatch(prompts);
        batchDao.updateBatchStatus(batchId, "IN_PROGRESS");
        for (String prompt : prompts) {
            workerPool.submit(() -> processPrompt(batchId, prompt));
        }
        return batchId;
    }

    public BatchStatusResponse getBatchStatus(String batchId) {
        BatchRecord record = batchDao.getBatch(batchId);
        if (record == null) {
            throw new IllegalArgumentException("batch not found");
        }
        return new BatchStatusResponse(record.getBatchId(), record.getStatus(), record.getTotalPrompts(), record.getCompleted(), record.getFailed());
    }

    public void processPrompt(String batchId, String prompt) {
        int retries = 0;
        while (retries < maxRetries) {
            try {
                InferenceResult result = inferenceClient.infer(prompt);
                batchDao.incrementCompleted(batchId);
                batchDao.updatePromptResult(batchId, prompt, result.response(), true);
                evaluateBatchCompletion(batchId);
                return;
            } catch (InferenceRateLimitException e) {
                retries++;
                if (retries >= maxRetries) {
                    batchDao.incrementFailed(batchId);
                    batchDao.updatePromptResult(batchId, prompt, "RATE_LIMITED", false);
                    evaluateBatchCompletion(batchId);
                    return;
                }
                sleepWithJitter(retries);
            } catch (Exception e) {
                batchDao.incrementFailed(batchId);
                batchDao.updatePromptResult(batchId, prompt, "ERROR: " + e.getMessage(), false);
                evaluateBatchCompletion(batchId);
                return;
            }
        }
    }

    private void evaluateBatchCompletion(String batchId) {
        BatchRecord record = batchDao.getBatch(batchId);
        if (record != null && record.getCompleted() + record.getFailed() >= record.getTotalPrompts()) {
            batchDao.updateBatchStatus(batchId, record.getFailed() == 0 ? "COMPLETED" : "FAILED");
        }
    }

    private void sleepWithJitter(int attempt) {
        long delay = (long) (initialBackoff.toMillis() * Math.pow(2, attempt - 1));
        long jitter = (long) (delay * jitterFactor * Math.random());
        try {
            Thread.sleep(delay + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
