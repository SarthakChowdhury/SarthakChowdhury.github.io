package com.example.inference.db;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
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
        jdbi.useExtension(Dao.class, dao -> dao.createSchema());
    }

    @Override
    public String createBatch(List<String> prompts) {
        String batchId = UUID.randomUUID().toString();
        jdbi.useExtension(Dao.class, dao -> dao.insertBatch(batchId, "IN_PROGRESS", prompts.size()));
        return batchId;
    }

    @Override
    public BatchRecord getBatch(String batchId) {
        return jdbi.withExtension(Dao.class, dao -> dao.findById(batchId));
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

    public interface Dao {
        @SqlUpdate("CREATE TABLE IF NOT EXISTS batches (batch_id VARCHAR(255) PRIMARY KEY, status VARCHAR(50), total_prompts INT, completed INT DEFAULT 0, failed INT DEFAULT 0)")
        void createSchema();

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
    }
}
