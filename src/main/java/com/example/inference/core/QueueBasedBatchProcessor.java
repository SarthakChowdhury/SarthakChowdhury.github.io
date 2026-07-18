package com.example.inference.core;

import com.example.inference.db.BatchDao;
import com.example.inference.model.InferenceResult;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

public class QueueBasedBatchProcessor {
    private final BatchDao batchDao;
    private final MockInferenceClient inferenceClient;
    private final ExecutorService workerPool;
    private final BlockingQueue<WorkItem> queue;
    private final int maxRetries;
    private final Duration initialBackoff;
    private final double jitterFactor;
    private final CountDownLatch completionLatch;
    private volatile boolean running;

    public QueueBasedBatchProcessor(BatchDao batchDao, MockInferenceClient inferenceClient, int workerCount, int queueCapacity, Duration initialBackoff, int maxRetries, double jitterFactor) {
        this.batchDao = batchDao;
        this.inferenceClient = inferenceClient;
        this.workerPool = Executors.newFixedThreadPool(workerCount);
        this.queue = new LinkedBlockingQueue<>(queueCapacity);
        this.maxRetries = maxRetries;
        this.initialBackoff = initialBackoff;
        this.jitterFactor = jitterFactor;
        this.completionLatch = new CountDownLatch(1);
        this.running = true;
        startWorkers();
    }

    public void submitBatch(String batchId, List<String> prompts) {
        for (String prompt : prompts) {
            queue.offer(new WorkItem(batchId, prompt));
        }
        completionLatch.countDown();
    }

    public void shutdown() {
        running = false;
        workerPool.shutdownNow();
    }

    private void startWorkers() {
        for (int i = 0; i < Math.max(1, Runtime.getRuntime().availableProcessors()); i++) {
            workerPool.submit(this::consumeLoop);
        }
    }

    private void consumeLoop() {
        while (running) {
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
        int retries = 0;
        while (retries < maxRetries) {
            try {
                InferenceResult result = inferenceClient.infer(item.prompt());
                batchDao.incrementCompleted(item.batchId());
                batchDao.updatePromptResult(item.batchId(), item.prompt(), result.response(), true);
                return;
            } catch (InferenceRateLimitException e) {
                retries++;
                if (retries >= maxRetries) {
                    batchDao.incrementFailed(item.batchId());
                    return;
                }
                sleepWithJitter(retries);
            } catch (Exception e) {
                batchDao.incrementFailed(item.batchId());
                return;
            }
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

    private record WorkItem(String batchId, String prompt) {
    }
}
