package com.redhat.kafka.consumer;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/api/status")
@Produces(MediaType.APPLICATION_JSON)
public class ConsumerStatusResource {

    @Inject
    CdcEventConsumer consumer;

    @GET
    public Map<String, Object> status() {
        return Map.of(
                "processed", consumer.getProcessedCount(),
                "errors", consumer.getErrorCount(),
                "status", "running"
        );
    }
}
