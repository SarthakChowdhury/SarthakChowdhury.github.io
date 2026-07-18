package com.example.inference.db;

import java.util.List;

public interface BatchDao {
    void initialize();
    String createBatch(List<String> prompts);
    BatchRecord getBatch(String batchId);
    void updateBatchStatus(String batchId, String status);
    void updatePromptResult(String batchId, String prompt, String response, boolean success);
    void incrementCompleted(String batchId);
    void incrementFailed(String batchId);
    List<BatchRecord> listBatches();
    List<BatchResultRecord> listBatchResults(String batchId);
}
