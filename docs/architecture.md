# Architecture

## 1. API ingestion and HTTP 202 acknowledgement

```mermaid
sequenceDiagram
    participant Client
    participant API as BatchResource
    participant DB as JdbiBatchDao
    participant Processor as QueueBasedBatchProcessor

    Client->>API: POST /v1/batches
    API->>API: Hibernate Validator on CreateBatchRequest
    API->>DB: insert batch status PENDING
    API->>Processor: submitBatch
    Processor->>DB: status IN_PROGRESS
    Processor->>Processor: put prompts on LinkedBlockingQueue
    API-->>Client: 202 Accepted batchId
```

## 2. Bounded worker pool and shared queue

```mermaid
flowchart LR
    Queue[LinkedBlockingQueue capacity from config]
    W1[Worker1]
    W2[Worker2]
    WN[WorkerN]
    Queue --> W1
    Queue --> W2
    Queue --> WN
    W1 --> Mock[MockInferenceClient]
    W2 --> Mock
    WN --> Mock
```

Workers are a fixed `ExecutorService` sized by `maxConcurrentWorkers` and started/stopped via Dropwizard `Managed` lifecycle. Consumers equal the pool size — never one thread per prompt.

## 3. Resilience4j retry on simulated HTTP 429

```mermaid
sequenceDiagram
    participant Worker
    participant Retry as Resilience4j Retry
    participant Mock as MockInferenceClient
    participant DB as JdbiBatchDao

    Worker->>Retry: decorate infer call
    Retry->>Mock: infer prompt
    alt success
        Mock-->>Retry: InferenceResult
        Retry-->>Worker: result
        Worker->>DB: incrementCompleted plus batch_results success
    else simulated 429
        Mock-->>Retry: InferenceRateLimitException
        Retry->>Retry: exponential backoff with jitter
        Retry->>Mock: retry until maxAttempts
        alt retries exhausted
            Retry-->>Worker: failure
            Worker->>DB: incrementFailed plus batch_results failure
        end
    end
```

## 4. Batch completion state transition

```mermaid
stateDiagram-v2
    [*] --> PENDING: createBatch
    PENDING --> IN_PROGRESS: submitBatch
    IN_PROGRESS --> COMPLETED: completed plus failed equals total and failed is 0
    IN_PROGRESS --> FAILED: completed plus failed equals total and failed greater than 0
```
