package com.example.inference.api;

import io.dropwizard.jersey.errors.ErrorMessage;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(title = "Batch Inference API", version = "1.0"),
        servers = @Server(url = "http://localhost:8080")
)
public class OpenApiConfiguration {
}
