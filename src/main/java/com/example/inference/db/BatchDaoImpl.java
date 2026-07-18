package com.example.inference.db;

import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class BatchDaoImpl implements BatchDao {
    private final BasicDataSource dataSource;

    public BatchDaoImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void initialize() {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS batches (batch_id VARCHAR(255) PRIMARY KEY, status VARCHAR(50), total_prompts INT, completed INT, failed INT)")) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String createBatch(List<String> prompts) {
        String batchId = UUID.randomUUID().toString();
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO batches(batch_id, status, total_prompts, completed, failed) VALUES (?, ?, ?, ?, ?)")) {
            statement.setString(1, batchId);
            statement.setString(2, "PENDING");
            statement.setInt(3, prompts.size());
            statement.setInt(4, 0);
            statement.setInt(5, 0);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return batchId;
    }

    @Override
    public BatchRecord getBatch(String batchId) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT batch_id, status, total_prompts, completed, failed FROM batches WHERE batch_id = ?")) {
            statement.setString(1, batchId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    BatchRecord record = new BatchRecord();
                    record.setBatchId(rs.getString("batch_id"));
                    record.setStatus(rs.getString("status"));
                    record.setTotalPrompts(rs.getInt("total_prompts"));
                    record.setCompleted(rs.getInt("completed"));
                    record.setFailed(rs.getInt("failed"));
                    return record;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public void updateBatchStatus(String batchId, String status) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE batches SET status = ? WHERE batch_id = ?")) {
            statement.setString(1, status);
            statement.setString(2, batchId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updatePromptResult(String batchId, String prompt, String response, boolean success) {
        // no-op placeholder for this simplified implementation
    }

    @Override
    public void incrementCompleted(String batchId) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE batches SET completed = completed + 1 WHERE batch_id = ?")) {
            statement.setString(1, batchId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void incrementFailed(String batchId) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE batches SET failed = failed + 1 WHERE batch_id = ?")) {
            statement.setString(1, batchId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<BatchRecord> listBatches() {
        return List.of();
    }

    @Override
    public List<BatchResultRecord> listBatchResults(String batchId) {
        return List.of();
    }
}
