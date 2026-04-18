package com.redhat.kafka.consumer;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CdcEventConsumer {

    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    @Inject
    MeterRegistry registry;

    @Incoming("cdc-events")
    public java.util.concurrent.CompletionStage<Void> consume(Message<String> message) {
        try {
            String payload = message.getPayload();
            Log.infof("Received CDC event (%d bytes): %s",
                    payload.length(),
                    payload.length() > 200 ? payload.substring(0, 200) + "..." : payload);

            processedCount.incrementAndGet();
            registry.counter("cdc_events_processed_total").increment();

            // TODO: Add your business logic here
            // Examples:
            //   - Parse the Debezium CDC JSON envelope
            //   - Extract before/after state
            //   - Apply transformations
            //   - Forward to another service or database

            return message.ack();
        } catch (Exception e) {
            errorCount.incrementAndGet();
            registry.counter("cdc_events_errors_total").increment();
            Log.errorf(e, "Failed to process CDC event");
            return message.nack(e);
        }
    }

    public long getProcessedCount() {
        return processedCount.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }
}
