package com.example.inference.resources;

import com.example.inference.api.BatchStatusResponse;
import com.example.inference.api.CreateBatchRequest;
import com.example.inference.api.CreateBatchResponse;
import com.example.inference.core.QueueBasedBatchProcessor;
import com.example.inference.db.BatchDao;
import com.example.inference.db.BatchRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class BatchHttpHandler implements HttpHandler {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern BATCH_PATH = Pattern.compile("^/v1/batches/([^/]+)$");

    private final BatchDao batchDao;
    private final QueueBasedBatchProcessor processor;

    public BatchHttpHandler(BatchDao batchDao, QueueBasedBatchProcessor processor) {
        this.batchDao = batchDao;
        this.processor = processor;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            System.out.println("[HTTP] " + method + " " + path);

                if ("POST".equals(method) && "/v1/batches".equals(path)) {
                handleCreateBatch(exchange);
                return;
            }

            if ("GET".equals(method) && "/admin/db".equals(path)) {
                handleAdminDb(exchange);
                return;
            }

            if ("GET".equals(method)) {
                var matcher = BATCH_PATH.matcher(path);
                if (matcher.matches()) {
                    handleGetBatch(exchange, matcher.group(1));
                    return;
                }
            }

            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleCreateBatch(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            CreateBatchRequest request = OBJECT_MAPPER.readValue(body, CreateBatchRequest.class);
            String batchId = batchDao.createBatch(request.getPrompts());
            processor.submitBatch(batchId, request.getPrompts());
            CreateBatchResponse response = new CreateBatchResponse(batchId, "IN_PROGRESS", "Batch accepted for processing");
            sendJson(exchange, 202, OBJECT_MAPPER.writeValueAsString(response));
        } catch (Exception e) {
            sendJson(exchange, 400, "{\"error\":\"Invalid request body\"}");
        }
    }

    private void handleGetBatch(HttpExchange exchange, String batchId) throws IOException {
        BatchRecord record = batchDao.getBatch(batchId);
        if (record == null) {
            sendJson(exchange, 404, "{\"error\":\"Batch not found\"}");
            return;
        }
        BatchStatusResponse response = new BatchStatusResponse(record.getBatchId(), record.getStatus(), record.getTotalPrompts(), record.getCompleted(), record.getFailed());
        sendJson(exchange, 200, OBJECT_MAPPER.writeValueAsString(response));
    }

    private void handleAdminDb(HttpExchange exchange) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("batches", batchDao.listBatches());
        payload.put("results", batchDao.listBatchResults(batchDao.listBatches().isEmpty() ? "" : batchDao.listBatches().get(0).getBatchId()));
        sendJson(exchange, 200, OBJECT_MAPPER.writeValueAsString(payload));
    }

    private void sendJson(HttpExchange exchange, int statusCode, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
