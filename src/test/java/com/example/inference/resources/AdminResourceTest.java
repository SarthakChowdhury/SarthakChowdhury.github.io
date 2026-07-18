package com.example.inference.resources;

import com.example.inference.db.BatchDao;
import com.example.inference.db.BatchRecord;
import com.example.inference.db.BatchResultRecord;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminResourceTest {

    @Test
    void adminEndpointReturnsBatchesAndResults() {
        StubBatchDao dao = new StubBatchDao();
        AdminResource resource = new AdminResource(dao);

        Response response = resource.inspect();

        assertThat(response.getStatus()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        var payload = (java.util.Map<String, Object>) response.getEntity();
        @SuppressWarnings("unchecked")
        var batches = (java.util.List<BatchRecord>) payload.get("batches");
        @SuppressWarnings("unchecked")
        var results = (java.util.List<BatchResultRecord>) payload.get("results");
        assertThat(batches).extracting(BatchRecord::getBatchId).contains("batch-1");
        assertThat(results).extracting(BatchResultRecord::getPrompt).contains("hello");
    }

    private static class StubBatchDao implements BatchDao {
        @Override
        public void initialize() {
        }

        @Override
        public String createBatch(List<String> prompts) {
            return "batch-1";
        }

        @Override
        public BatchRecord getBatch(String batchId) {
            return null;
        }

        @Override
        public void updateBatchStatus(String batchId, String status) {
        }

        @Override
        public void updatePromptResult(String batchId, String prompt, String response, boolean success) {
        }

        @Override
        public void incrementCompleted(String batchId) {
        }

        @Override
        public void incrementFailed(String batchId) {
        }

        @Override
        public List<BatchRecord> listBatches() {
            BatchRecord record = new BatchRecord();
            record.setBatchId("batch-1");
            record.setStatus("IN_PROGRESS");
            record.setTotalPrompts(2);
            record.setCompleted(1);
            record.setFailed(0);
            return List.of(record);
        }

        @Override
        public List<BatchResultRecord> listBatchResults(String batchId) {
            BatchResultRecord result = new BatchResultRecord();
            result.setBatchId(batchId);
            result.setPrompt("hello");
            result.setResponse("mock-response");
            result.setSuccess(true);
            return List.of(result);
        }
    }
}
