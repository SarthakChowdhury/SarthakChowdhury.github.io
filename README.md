# Batch Inference Service

A Dropwizard-based backend for accepting large prompt batches, processing them concurrently, and tracking their status with retry and backoff behavior.

## Quickstart

```bash
mvn clean install
java -jar target/batch-inference-service-1.0.0-SNAPSHOT.jar server config.yml
```

## API

### Create a batch

```bash
curl -X POST http://localhost:8080/v1/batches \
  -H 'Content-Type: application/json' \
  -d '{"prompts":["hello","world","dropwizard"]}'
```

### Check batch status

```bash
curl http://localhost:8080/v1/batches/<batch-id>
```

## Concurrency model

The service uses a bounded worker pool sized by config and a shared queue-like submission approach to process prompts without spawning unbounded threads. Resilience4j-style retry with exponential backoff and jitter is applied around a mock inference client that randomly simulates HTTP 429 responses.
