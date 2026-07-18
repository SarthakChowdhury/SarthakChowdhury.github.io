package com.example.inference.core;

import com.example.inference.api.BatchStatusResponse;
import com.example.inference.db.BatchDao;
import com.example.inference.db.BatchDaoImpl;
import com.example.inference.db.BatchRecord;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BatchProcessorServiceTest {

    @Test
    void retryTriggersAndBatchEventuallyCompletes() throws Exception {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:batch-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        BatchDao dao = new BatchDaoImpl(dataSource);
        dao.initialize();

        MockInferenceClient client = new MockInferenceClient(0.0, 50, 0);
        BatchProcessorService service = new BatchProcessorService(dao, client, 2, Duration.ofMillis(10), 3, 0.0);
        String batchId = service.createBatch(List.of("alpha", "beta"));

        Thread.sleep(1000);

        BatchStatusResponse status = service.getBatchStatus(batchId);
        assertThat(status.status()).isEqualTo("COMPLETED");
        assertThat(status.completed()).isEqualTo(2);
        assertThat(status.failed()).isEqualTo(0);
    }
}
