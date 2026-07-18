package com.example.inference;

import com.example.inference.api.OpenApiConfiguration;
import com.example.inference.core.MockInferenceClient;
import com.example.inference.core.QueueBasedBatchProcessor;
import com.example.inference.db.BatchDao;
import com.example.inference.db.JdbiBatchDao;
import com.example.inference.exception.ValidationExceptionMapper;
import com.example.inference.resources.BatchHttpHandler;
import org.jdbi.v3.core.Jdbi;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpServer;

public class InferenceApplication {
    public static void main(String[] args) throws Exception {
        BatchProcessorConfiguration configuration = new BatchProcessorConfiguration();
        Jdbi jdbi = JdbiBatchDao.createJdbi(configuration.getDbUrl(), configuration.getDbUser(), configuration.getDbPassword());
        BatchDao batchDao = new JdbiBatchDao(jdbi);
        batchDao.initialize();

        MockInferenceClient inferenceClient = new MockInferenceClient(0.15, 150, 7);
        QueueBasedBatchProcessor processor = new QueueBasedBatchProcessor(
                batchDao,
                inferenceClient,
                configuration.getMaxConcurrentWorkers(),
                100,
                Duration.ofMillis(configuration.getRetryInitialDelayMs()),
                configuration.getMaxRetries(),
                configuration.getRetryJitterFactor()
        );

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new BatchHttpHandler(batchDao, processor));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Server started on http://127.0.0.1:8080/");
    }
}
