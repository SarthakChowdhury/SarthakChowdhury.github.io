package com.example.inference.resources;

import com.example.inference.api.BatchStatusResponse;
import com.example.inference.api.CreateBatchRequest;
import com.example.inference.api.CreateBatchResponse;
import com.example.inference.core.QueueBasedBatchProcessor;
import com.example.inference.db.BatchDao;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

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
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Batch not found"))
                    .build();
        }
        BatchStatusResponse response = new BatchStatusResponse(
                record.getBatchId(),
                record.getStatus(),
                record.getTotalPrompts(),
                record.getCompleted(),
                record.getFailed()
        );
        return Response.ok(response).build();
    }
}
