package com.example.inference;

import com.example.inference.controller.InferenceController;
import com.example.inference.model.BatchInferenceRequest;
import com.example.inference.model.BatchInferenceResponse;
import com.example.inference.model.InferenceResult;
import com.example.inference.service.InferenceService;
import com.example.inference.service.client.MockRateLimitedInferenceClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InferenceResourceTest {

    @Test
    void batchEndpointReturnsOneResultPerPrompt() {
        InferenceController controller = new InferenceController(new InferenceService(java.util.concurrent.Executors.newFixedThreadPool(2), new MockRateLimitedInferenceClient(4, 250)));
        BatchInferenceRequest request = new BatchInferenceRequest(java.util.List.of("hello", "world"));

        var response = controller.runBatch(request);

        assertThat(response.getStatus()).isEqualTo(200);
        var entity = (BatchInferenceResponse) response.getEntity();
        assertThat(entity.results()).hasSize(2);
        assertThat(entity.results()).extracting(InferenceResult::prompt).containsExactly("hello", "world");
    }
}
