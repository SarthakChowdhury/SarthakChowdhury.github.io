package com.example.inference.core;

import com.example.inference.db.BatchDao;
import com.example.inference.db.BatchRecord;
import com.example.inference.db.BatchResultRecord;
import com.example.inference.db.JdbiBatchDao;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class QueueBasedBatchProcessorTest {

    @Test
    void batchStatusBecomesCompletedWhenAllPromptsSucceed() throws Exception {
        BatchDao dao = newInMemoryDao("queue-batch-complete");
        MockInferenceClient client = new MockInferenceClient(0.0, 10, 10);
        QueueBasedBatchProcessor processor = new QueueBasedBatchProcessor(
                dao, client, 2, 100, Duration.ofMillis(10), 5, 0.2
        );
        processor.start();

        try {
            String batchId = dao.createBatch(List.of("alpha", "beta"));
            assertThat(dao.getBatch(batchId).getStatus()).isEqualTo("PENDING");
            processor.submitBatch(batchId, List.of("alpha", "beta"));
            assertThat(dao.getBatch(batchId).getStatus()).isEqualTo("IN_PROGRESS");

            waitUntil(Duration.ofSeconds(3), () -> {
                BatchRecord record = dao.getBatch(batchId);
                return "COMPLETED".equals(record.getStatus());
            });

            BatchRecord record = dao.getBatch(batchId);
            assertThat(record.getStatus()).isEqualTo("COMPLETED");
            assertThat(record.getCompleted()).isEqualTo(2);
            assertThat(record.getFailed()).isEqualTo(0);
        } finally {
            processor.stop();
        }
    }

    @Test
    void resilience4jRetriesExhaustionMarksPromptFailed() throws Exception {
        BatchDao dao = newInMemoryDao("queue-batch-fail");
        MockInferenceClient client = new MockInferenceClient(1.0, 5, 5);
        QueueBasedBatchProcessor processor = new QueueBasedBatchProcessor(
                dao, client, 2, 100, Duration.ofMillis(20), 3, 0.1
        );
        processor.start();

        try {
            String batchId = dao.createBatch(List.of("only"));
            processor.submitBatch(batchId, List.of("only"));

            waitUntil(Duration.ofSeconds(5), () -> {
                BatchRecord record = dao.getBatch(batchId);
                return "FAILED".equals(record.getStatus());
            });

            BatchRecord record = dao.getBatch(batchId);
            assertThat(record.getFailed()).isEqualTo(1);
            assertThat(record.getCompleted()).isEqualTo(0);
            List<BatchResultRecord> results = dao.listBatchResults(batchId);
            assertThat(results).hasSize(1);
            assertThat(results.get(0).isSuccess()).isFalse();
        } finally {
            processor.stop();
        }
    }

    @Test
    void resilience4jRetryEventuallyCompletesDespiteRateLimits() throws Exception {
        BatchDao dao = newInMemoryDao("queue-batch-retry");
        MockInferenceClient client = new MockInferenceClient(0.6, 5, 5);
        QueueBasedBatchProcessor processor = new QueueBasedBatchProcessor(
                dao, client, 4, 100, Duration.ofMillis(25), 8, 0.2
        );
        processor.start();

        try {
            String batchId = dao.createBatch(List.of("a", "b", "c"));
            processor.submitBatch(batchId, List.of("a", "b", "c"));

            waitUntil(Duration.ofSeconds(15), () -> {
                BatchRecord record = dao.getBatch(batchId);
                return "COMPLETED".equals(record.getStatus()) || "FAILED".equals(record.getStatus());
            });

            BatchRecord record = dao.getBatch(batchId);
            assertThat(record.getCompleted() + record.getFailed()).isEqualTo(3);
            assertThat(record.getStatus()).isIn("COMPLETED", "FAILED");
            // With 8 attempts and 60% rate-limit chance, success is the common outcome.
            assertThat(record.getCompleted()).isGreaterThanOrEqualTo(1);
        } finally {
            processor.stop();
        }
    }

    @Test
    void submitBatchDoesNotDropPromptsUpToConfiguredCapacity() throws Exception {
        BatchDao dao = newInMemoryDao("queue-batch-capacity");
        AtomicInteger inferences = new AtomicInteger();
        MockInferenceClient client = new MockInferenceClient(0.0, 1, 1) {
            @Override
            public com.example.inference.model.InferenceResult infer(String prompt) throws Exception {
                inferences.incrementAndGet();
                return super.infer(prompt);
            }
        };
        QueueBasedBatchProcessor processor = new QueueBasedBatchProcessor(
                dao, client, 4, 1000, Duration.ofMillis(5), 3, 0.0
        );
        processor.start();

        try {
            List<String> prompts = java.util.stream.IntStream.range(0, 200)
                    .mapToObj(i -> "p" + i)
                    .toList();
            String batchId = dao.createBatch(prompts);
            processor.submitBatch(batchId, prompts);

            waitUntil(Duration.ofSeconds(20), () -> {
                BatchRecord record = dao.getBatch(batchId);
                return record.getCompleted() + record.getFailed() >= 200;
            });

            assertThat(inferences.get()).isEqualTo(200);
            assertThat(dao.getBatch(batchId).getStatus()).isEqualTo("COMPLETED");
        } finally {
            processor.stop();
        }
    }

    private static BatchDao newInMemoryDao(String name) {
        Jdbi jdbi = JdbiBatchDao.createJdbi(
                "jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        BatchDao dao = new JdbiBatchDao(jdbi);
        dao.initialize();
        return dao;
    }

    private static void waitUntil(Duration timeout, java.util.concurrent.Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(condition.call())) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Condition not met within " + timeout);
    }
}
