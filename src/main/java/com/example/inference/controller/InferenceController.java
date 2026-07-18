package com.example.inference.controller;

import com.example.inference.model.BatchInferenceRequest;
import com.example.inference.service.InferenceService;
import com.example.inference.model.BatchInferenceResponse;
import com.example.inference.model.ErrorResponse;
import com.example.inference.model.InferenceResult;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Path("/inference")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InferenceController {

    private final InferenceService inferenceService;

    public InferenceController(InferenceService inferenceService) {
        this.inferenceService = inferenceService;
    }

    @POST
    @Path("/batch")
    public Response runBatch(BatchInferenceRequest request) {
        if (request == null || request.prompts() == null || request.prompts().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("prompts must not be empty"))
                    .build();
        }

        try {
            List<InferenceResult> results = inferenceService.runBatch(request.prompts());
            return Response.ok(new BatchInferenceResponse(results)).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Response.serverError().entity(new ErrorResponse("request interrupted")).build();
        } catch (ExecutionException e) {
            return Response.serverError().entity(new ErrorResponse("inference failed")).build();
        }
    }
}
