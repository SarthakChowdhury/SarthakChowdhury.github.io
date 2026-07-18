package com.example.inference.resources;

import com.example.inference.db.BatchDao;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("/admin/db")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {
    private final BatchDao batchDao;

    public AdminResource(BatchDao batchDao) {
        this.batchDao = batchDao;
    }

    @GET
    public Response inspect() {
        Map<String, Object> payload = new LinkedHashMap<>();
        var batches = batchDao.listBatches();
        payload.put("batches", batches);
        payload.put("results", batches.isEmpty()
                ? java.util.List.of()
                : batchDao.listBatchResults(batches.get(0).getBatchId()));
        return Response.ok(payload).build();
    }
}
