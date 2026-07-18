package com.example.inference.resources;

import com.example.inference.core.QueueBasedBatchProcessor;
import com.example.inference.db.BatchDao;
import com.example.inference.db.BatchRecord;
import com.example.inference.db.BatchResultRecord;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BatchHttpHandlerTest {

    @Test
    void adminEndpointReturnsBatchesAndResults() throws Exception {
        StubBatchDao dao = new StubBatchDao();
        BatchHttpHandler handler = new BatchHttpHandler(dao, null);
        StubHttpExchange exchange = new StubHttpExchange("GET", "/admin/db");

        handler.handle(exchange);

        assertThat(exchange.getResponseCode()).isEqualTo(200);
        String body = exchange.getResponseBodyAsString();
        assertThat(body).contains("batchId");
        assertThat(body).contains("hello");
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

    private static class StubHttpExchange extends HttpExchange {
        private final String method;
        private final URI uri;
        private final Headers requestHeaders = new Headers();
        private final Headers responseHeaders = new Headers();
        private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        private int responseCode = 200;

        private StubHttpExchange(String method, String path) {
            this.method = method;
            this.uri = URI.create(path);
        }

        @Override
        public Headers getRequestHeaders() {
            return requestHeaders;
        }

        @Override
        public Headers getResponseHeaders() {
            return responseHeaders;
        }

        @Override
        public URI getRequestURI() {
            return uri;
        }

        @Override
        public String getRequestMethod() {
            return method;
        }

        @Override
        public HttpContext getHttpContext() {
            return null;
        }

        @Override
        public InputStream getRequestBody() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public OutputStream getResponseBody() {
            return responseBody;
        }

        @Override
        public void sendResponseHeaders(int i, long l) throws IOException {
            this.responseCode = i;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public String getProtocol() {
            return "HTTP/1.1";
        }

        @Override
        public Object getAttribute(String s) {
            return null;
        }

        @Override
        public void setAttribute(String s, Object o) {
        }

        @Override
        public HttpPrincipal getPrincipal() {
            return null;
        }

        @Override
        public void setStreams(InputStream inputStream, OutputStream outputStream) {
        }

        @Override
        public void close() {
        }

        public int getResponseCode() {
            return responseCode;
        }

        public String getResponseBodyAsString() {
            return responseBody.toString();
        }
    }
}
