package com.example.inference.core;

import com.example.inference.db.BatchDao;
import com.example.inference.db.BatchRecord;
import com.example.inference.model.InferenceResult;
import io.dropwizard.lifecycle.Managed;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class QueueBasedBatchProcessor implements Managed {
    private final BatchDao batchDao;
    private final MockInferenceClient inferenceClient;
    private final ExecutorService workerPool;
    private final BlockingQueue<WorkItem> queue;
    private final int workerCount;
    private final Retry retry;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public QueueBasedBatchProcessor(
            BatchDao batchDao,
            MockInferenceClient inferenceClient,
            int workerCount,
            int queueCapacity,
            Duration initialBackoff,
            int maxAttempts,
            double jitterFactor
    ) {
        this.batchDao = batchDao;
        this.inferenceClient = inferenceClient;
        this.workerCount = Math.max(1, workerCount);
        this.workerPool = Executors.newFixedThreadPool(this.workerCount);
        this.queue = new LinkedBlockingQueue<>(Math.max(this.workerCount, queueCapacity));

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(Math.max(1, maxAttempts))
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                        initialBackoff,
                        2.0d,
                        Math.max(0.0d, Math.min(1.0d, jitterFactor))
                ))
                .retryOnException(throwable -> throwable instanceof InferenceRateLimitException)
                .failAfterMaxAttempts(true)
                .build();
        this.retry = RetryRegistry.of(retryConfig).retry("inference-retry");
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        for (int i = 0; i < workerCount; i++) {
            workerPool.submit(this::consumeLoop);
        }
    }

    @Override
    public void stop() {
        running.set(false);
        workerPool.shutdownNow();
    }

    public void submitBatch(String batchId, List<String> prompts) {
        batchDao.updateBatchStatus(batchId, "IN_PROGRESS");
        for (String prompt : prompts) {
            try {
                queue.put(new WorkItem(batchId, prompt));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while enqueueing prompt for batch " + batchId, e);
            }
        }
    }

    /** Prefer Dropwizard Managed lifecycle stop(). */
    @Deprecated
    public void shutdown() {
        stop();
    }

    private void consumeLoop() {
        while (running.get()) {
            try {
                WorkItem item = queue.poll(250, TimeUnit.MILLISECONDS);
                if (item == null) {
                    continue;
                }
                process(item);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void process(WorkItem item) {
        try {
            InferenceResult result = Retry.decorateCheckedSupplier(retry, () -> inferenceClient.infer(item.prompt())).get();
            batchDao.incrementCompleted(item.batchId());
            batchDao.updatePromptResult(item.batchId(), item.prompt(), result.response(), true);
            evaluateBatchCompletion(item.batchId());
        } catch (InferenceRateLimitException e) {
            batchDao.incrementFailed(item.batchId());
            batchDao.updatePromptResult(item.batchId(), item.prompt(), "RATE_LIMITED: " + e.getMessage(), false);
            evaluateBatchCompletion(item.batchId());
        } catch (Throwable e) {
            batchDao.incrementFailed(item.batchId());
            batchDao.updatePromptResult(item.batchId(), item.prompt(), "ERROR: " + e.getMessage(), false);
            evaluateBatchCompletion(item.batchId());
        }
    }

    private void evaluateBatchCompletion(String batchId) {
        BatchRecord record = batchDao.getBatch(batchId);
        if (record != null && record.getCompleted() + record.getFailed() >= record.getTotalPrompts()) {
            batchDao.updateBatchStatus(batchId, record.getFailed() == 0 ? "COMPLETED" : "FAILED");
        }
    }

    private record WorkItem(String batchId, String prompt) {
    }
}
