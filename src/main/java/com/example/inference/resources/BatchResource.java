package com.example.inference.resources;

import com.example.inference.api.BatchStatusResponse;
import com.example.inference.api.CreateBatchRequest;
import com.example.inference.api.CreateBatchResponse;
import com.example.inference.core.QueueBasedBatchProcessor;
import com.example.inference.db.BatchDao;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/batches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BatchResource {
    private final BatchDao batchDao;
    private final QueueBasedBatchProcessor processor;

    public BatchResource(BatchDao batchDao, QueueBasedBatchProcessor processor) {
        this.batchDao = batchDao;
        this.processor = processor;
    }

    @POST
    public Response createBatch(@Valid CreateBatchRequest request) {
        String batchId = batchDao.createBatch(request.getPrompts());
        processor.submitBatch(batchId, request.getPrompts());
        return Response.accepted(new CreateBatchResponse(batchId, "IN_PROGRESS", "Batch accepted for processing")).build();
    }

    @GET
    @Path("/{batchId}")
    public Response getBatchStatus(@PathParam("batchId") String batchId) {
        var record = batchDao.getBatch(batchId);
        if (record == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        BatchStatusResponse response = new BatchStatusResponse(record.getBatchId(), record.getStatus(), record.getTotalPrompts(), record.getCompleted(), record.getFailed());
        return Response.ok(response).build();
    }
}
