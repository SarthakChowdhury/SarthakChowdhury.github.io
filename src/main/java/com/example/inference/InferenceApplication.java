package com.example.inference;

import com.example.inference.core.MockInferenceClient;
import com.example.inference.core.QueueBasedBatchProcessor;
import com.example.inference.db.BatchDao;
import com.example.inference.db.JdbiBatchDao;
import com.example.inference.exception.ValidationExceptionMapper;
import com.example.inference.resources.AdminResource;
import com.example.inference.resources.BatchResource;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.jdbi.v3.core.Jdbi;

import java.time.Duration;

public class InferenceApplication extends Application<BatchProcessorConfiguration> {

    public static void main(String[] args) throws Exception {
        new InferenceApplication().run(args);
    }

    @Override
    public String getName() {
        return "batch-inference-service";
    }

    @Override
    public void initialize(Bootstrap<BatchProcessorConfiguration> bootstrap) {
        // no additional bundles required
    }

    @Override
    public void run(BatchProcessorConfiguration configuration, Environment environment) {
        Jdbi jdbi = JdbiBatchDao.createJdbi(
                configuration.getDbUrl(),
                configuration.getDbUser(),
                configuration.getDbPassword()
        );
        BatchDao batchDao = new JdbiBatchDao(jdbi);
        batchDao.initialize();

        MockInferenceClient inferenceClient = new MockInferenceClient(
                configuration.getRateLimitChance(),
                configuration.getMockLatencyMinMs(),
                configuration.getMockLatencyMaxMs()
        );

        QueueBasedBatchProcessor processor = new QueueBasedBatchProcessor(
                batchDao,
                inferenceClient,
                configuration.getMaxConcurrentWorkers(),
                configuration.getQueueCapacity(),
                Duration.ofMillis(configuration.getRetryInitialDelayMs()),
                configuration.getMaxRetries(),
                configuration.getRetryJitterFactor()
        );

        environment.lifecycle().manage(processor);
        environment.jersey().register(new ValidationExceptionMapper());
        environment.jersey().register(new BatchResource(batchDao, processor));
        environment.jersey().register(new AdminResource(batchDao));
    }
}
