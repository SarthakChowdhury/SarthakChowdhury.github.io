package com.example.inference.resources;

import com.example.inference.api.CreateBatchRequest;
import com.example.inference.core.MockInferenceClient;
import com.example.inference.core.QueueBasedBatchProcessor;
import com.example.inference.db.BatchDao;
import com.example.inference.db.BatchRecord;
import com.example.inference.db.JdbiBatchDao;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BatchIngestionNonBlockingTest {

    private QueueBasedBatchProcessor processor;
    private BatchDao dao;
    private BatchResource resource;

    @BeforeEach
    void setUp() throws Exception {
        Jdbi jdbi = JdbiBatchDao.createJdbi(
                "jdbc:h2:mem:nonblocking;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        dao = new JdbiBatchDao(jdbi);
        dao.initialize();
        // Slow mock so processing cannot finish before HTTP response returns.
        MockInferenceClient client = new MockInferenceClient(0.0, 300, 300);
        processor = new QueueBasedBatchProcessor(
                dao, client, 2, 100, Duration.ofMillis(10), 3, 0.0
        );
        processor.start();
        resource = new BatchResource(dao, processor);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (processor != null) {
            processor.stop();
        }
    }

    @Test
    void createBatchReturnsImmediatelyWhileWorkContinues() {
        CreateBatchRequest request = new CreateBatchRequest();
        request.setPrompts(List.of("slow-one", "slow-two"));

        long started = System.nanoTime();
        var response = resource.createBatch(request);
        long elapsedMs = Duration.ofNanos(System.nanoTime() - started).toMillis();

        assertThat(response.getStatus()).isEqualTo(202);
        assertThat(elapsedMs).isLessThan(250);

        String batchId = ((com.example.inference.api.CreateBatchResponse) response.getEntity()).getBatchId();
        BatchRecord immediate = dao.getBatch(batchId);
        assertThat(immediate.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(immediate.getCompleted() + immediate.getFailed()).isLessThan(2);
    }
}
