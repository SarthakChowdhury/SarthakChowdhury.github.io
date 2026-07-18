package com.example.inference.db;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.UUID;

public class JdbiBatchDao implements BatchDao {
    private final Jdbi jdbi;

    public JdbiBatchDao(Jdbi jdbi) {
        this.jdbi = jdbi;
        this.jdbi.installPlugin(new SqlObjectPlugin());
    }

    public static Jdbi createJdbi(String url, String user, String password) {
        return Jdbi.create(url, user, password);
    }

    @Override
    public void initialize() {
        System.out.println("[DB] Initializing schema");
        jdbi.useExtension(Dao.class, dao -> {
            dao.createSchema();
            dao.reconcileTerminalStatuses();
        });
    }

    @Override
    public String createBatch(List<String> prompts) {
        String batchId = UUID.randomUUID().toString();
        System.out.println("[DB] Creating batch " + batchId + " with " + prompts.size() + " prompts");
        jdbi.useExtension(Dao.class, dao -> dao.insertBatch(batchId, "PENDING", prompts.size()));
        System.out.println("[DB] Batch inserted: " + batchId);
        return batchId;
    }

    @Override
    public BatchRecord getBatch(String batchId) {
        System.out.println("[DB] Looking up batch " + batchId);
        BatchRecord record = jdbi.withExtension(Dao.class, dao -> dao.findById(batchId));
        if (record == null) {
            System.out.println("[DB] Batch not found: " + batchId);
        } else {
            System.out.println("[DB] Batch found: " + batchId + " status=" + record.getStatus() + " completed=" + record.getCompleted() + " failed=" + record.getFailed());
        }
        return record;
    }

    @Override
    public void updateBatchStatus(String batchId, String status) {
        jdbi.useExtension(Dao.class, dao -> dao.updateStatus(batchId, status));
    }

    @Override
    public void incrementCompleted(String batchId) {
        jdbi.useExtension(Dao.class, dao -> dao.incrementCompleted(batchId));
    }

    @Override
    public void incrementFailed(String batchId) {
        jdbi.useExtension(Dao.class, dao -> dao.incrementFailed(batchId));
    }

    @Override
    public void updatePromptResult(String batchId, String prompt, String response, boolean success) {
        jdbi.useExtension(Dao.class, dao -> dao.updatePromptResult(batchId, prompt, response, success));
    }

    @Override
    public List<BatchRecord> listBatches() {
        return jdbi.withExtension(Dao.class, Dao::listBatches);
    }

    @Override
    public List<BatchResultRecord> listBatchResults(String batchId) {
        return jdbi.withExtension(Dao.class, dao -> dao.listBatchResults(batchId));
    }

    @RegisterBeanMapper(BatchRecord.class)
    @RegisterBeanMapper(BatchResultRecord.class)
    public interface Dao {
        @SqlUpdate("CREATE TABLE IF NOT EXISTS batches (batch_id VARCHAR(255) PRIMARY KEY, status VARCHAR(50), total_prompts INT, completed INT DEFAULT 0, failed INT DEFAULT 0); CREATE TABLE IF NOT EXISTS batch_results (batch_id VARCHAR(255), prompt VARCHAR(1000), response VARCHAR(1000), success BOOLEAN)")
        void createSchema();

        @SqlUpdate("UPDATE batches SET status = CASE WHEN failed = 0 THEN 'COMPLETED' ELSE 'FAILED' END WHERE status = 'IN_PROGRESS' AND completed + failed >= total_prompts")
        void reconcileTerminalStatuses();

        @SqlUpdate("INSERT INTO batches(batch_id, status, total_prompts, completed, failed) VALUES (?, ?, ?, 0, 0)")
        void insertBatch(@Bind("batchId") String batchId, @Bind("status") String status, @Bind("totalPrompts") int totalPrompts);

        @SqlQuery("SELECT batch_id AS batchId, status, total_prompts AS totalPrompts, completed, failed FROM batches WHERE batch_id = :batchId")
        BatchRecord findById(@Bind("batchId") String batchId);

        @SqlUpdate("UPDATE batches SET status = :status WHERE batch_id = :batchId")
        void updateStatus(@Bind("batchId") String batchId, @Bind("status") String status);

        @SqlUpdate("UPDATE batches SET completed = completed + 1 WHERE batch_id = :batchId")
        void incrementCompleted(@Bind("batchId") String batchId);

        @SqlUpdate("UPDATE batches SET failed = failed + 1 WHERE batch_id = :batchId")
        void incrementFailed(@Bind("batchId") String batchId);

        @SqlUpdate("INSERT INTO batch_results(batch_id, prompt, response, success) VALUES (:batchId, :prompt, :response, :success)")
        void updatePromptResult(@Bind("batchId") String batchId, @Bind("prompt") String prompt, @Bind("response") String response, @Bind("success") boolean success);

        @SqlQuery("SELECT batch_id AS batchId, status, total_prompts AS totalPrompts, completed, failed FROM batches ORDER BY batch_id")
        List<BatchRecord> listBatches();

        @SqlQuery("SELECT batch_id AS batchId, prompt, response, success FROM batch_results WHERE batch_id = :batchId ORDER BY prompt")
        List<BatchResultRecord> listBatchResults(@Bind("batchId") String batchId);
    }
}
