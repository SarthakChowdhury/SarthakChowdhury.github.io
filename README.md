# Batch Inference Service

Dropwizard 4 + Java 21 service that accepts large AI prompt batches, processes them concurrently with a bounded worker pool, retries simulated HTTP 429 responses with Resilience4j, and exposes batch status over REST.

## Quickstart

```bash
mvn clean install
java -jar target/batch-inference-service-1.0.0-SNAPSHOT.jar server config.yml
```

The server listens on `http://127.0.0.1:8080` (admin port `8081`). Default `config.yml` uses file-backed H2 in PostgreSQL compatibility mode so you can run locally without Docker.

### Optional PostgreSQL

```bash
docker compose up -d
```

Then point `config.yml` at Postgres:

```yaml
dbUrl: jdbc:postgresql://localhost:5432/batchdb
dbUser: batchuser
dbPassword: batchpass
```

## API

### Create a batch

```bash
curl -s -X POST http://127.0.0.1:8080/v1/batches \
  -H 'Content-Type: application/json' \
  -d '{"prompts":["hello","world"]}'
```

Returns **202 Accepted** with a `batchId` immediately while work continues in the background.

### Check batch status

```bash
curl -s http://127.0.0.1:8080/v1/batches/<batch-id>
```

Statuses: `PENDING` → `IN_PROGRESS` → `COMPLETED` or `FAILED`.

### Admin DB inspect

```bash
curl -s http://127.0.0.1:8080/admin/db
```

## Concurrency model

- Fixed worker pool sized by `maxConcurrentWorkers` (Dropwizard `Managed` lifecycle).
- Prompts are enqueued on a bounded `LinkedBlockingQueue` (`queueCapacity`, default 1000) using blocking `put` so prompts are not dropped.
- Workers pull work in a consumer loop — the service never starts one thread per prompt.

## Resilience4j retry

Each inference call is wrapped in a Resilience4j `Retry` registry entry configured from YAML:

- `maxRetries` — max attempts per prompt (includes the first try)
- `retryInitialDelayMs` — initial backoff
- multiplier `2.0` with randomized jitter (`retryJitterFactor`) via `IntervalFunction.ofExponentialRandomBackoff`
- retries only on `InferenceRateLimitException` (mock HTTP 429)

After attempts are exhausted the prompt is marked failed and persisted in `batch_results`.

## Tests

```bash
mvn clean test
```

Coverage includes non-blocking ingestion, H2-backed completion, and Resilience4j retry exhaustion / recovery scenarios.
