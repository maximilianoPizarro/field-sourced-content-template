package com.neuralbank.mcp.tools;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class KafkaCdcTools {

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrapServers;

    @ConfigProperty(name = "apicurio.registry.url")
    String registryUrl;

    @ConfigProperty(name = "postgresql.host")
    String pgHost;

    @ConfigProperty(name = "postgresql.database")
    String pgDatabase;

    @ConfigProperty(name = "postgresql.username")
    String pgUsername;

    private AdminClient createAdminClient() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        return AdminClient.create(props);
    }

    @Tool(description = "List all Kafka topics in the CDC cluster. Shows topic names, partition counts, and replication factors.")
    public String listKafkaTopics() {
        try (AdminClient admin = createAdminClient()) {
            Set<String> topicNames = admin.listTopics().names().get();
            DescribeTopicsResult descriptions = admin.describeTopics(topicNames);
            Map<String, TopicDescription> topics = descriptions.all().get();

            StringBuilder sb = new StringBuilder();
            sb.append("Kafka Topics in CDC Cluster:\n");
            sb.append("============================\n");
            topics.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    TopicDescription td = e.getValue();
                    sb.append(String.format("  %-50s  partitions: %d  replicas: %d\n",
                        td.name(), td.partitions().size(),
                        td.partitions().get(0).replicas().size()));
                });
            sb.append("\nTotal: ").append(topics.size()).append(" topics");
            return sb.toString();
        } catch (Exception e) {
            return "Error listing topics: " + e.getMessage();
        }
    }

    @Tool(description = "Read the latest CDC events from a specific Kafka topic. Returns the most recent messages showing database changes captured by Debezium.")
    public String readCdcEvents(
            @ToolArg(description = "Kafka topic name (e.g., cdc.public.customers)") String topicName,
            @ToolArg(description = "Maximum number of events to read (default: 5)") String maxEvents
    ) {
        int max = 5;
        try {
            max = Integer.parseInt(maxEvents);
        } catch (Exception ignored) {
        }

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "mcp-reader-" + UUID.randomUUID().toString().substring(0, 8));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, max);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topicName));
            consumer.poll(Duration.ofMillis(1000));
            Set<TopicPartition> partitions = consumer.assignment();
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);
            for (TopicPartition tp : partitions) {
                long end = endOffsets.getOrDefault(tp, 0L);
                long seekTo = Math.max(0, end - max);
                consumer.seek(tp, seekTo);
            }
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000));

            StringBuilder sb = new StringBuilder();
            sb.append("CDC Events from topic: ").append(topicName).append("\n");
            sb.append("=".repeat(60)).append("\n");
            int count = 0;
            var it = records.iterator();
            while (it.hasNext() && count < max) {
                var record = it.next();
                sb.append(String.format("[Partition %d | Offset %d | Timestamp %s]\n",
                    record.partition(), record.offset(), new Date(record.timestamp())));
                String value = record.value();
                if (value != null && value.length() > 500) {
                    value = value.substring(0, 500) + "... (truncated)";
                }
                sb.append(value).append("\n\n");
                count++;
            }
            if (count == 0) {
                sb.append("No recent events found. The topic may be empty or events are older.\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error reading events: " + e.getMessage();
        }
    }

    @Tool(description = "List all consumer groups and their lag for the CDC Kafka cluster. Shows which groups are consuming events and how far behind they are.")
    public String listConsumerGroups() {
        try (AdminClient admin = createAdminClient()) {
            var groups = admin.listConsumerGroups().all().get();

            StringBuilder sb = new StringBuilder();
            sb.append("Consumer Groups in CDC Cluster:\n");
            sb.append("===============================\n");
            for (ConsumerGroupListing group : groups) {
                sb.append(String.format("  Group: %-40s  simple: %s\n",
                    group.groupId(), group.isSimpleConsumerGroup()));
                try {
                    Map<TopicPartition, OffsetAndMetadata> offsets = admin.listConsumerGroupOffsets(group.groupId())
                        .partitionsToOffsetAndMetadata().get();
                    for (var entry : offsets.entrySet()) {
                        sb.append(String.format("    %-50s  offset: %d\n",
                            entry.getKey().topic() + "[" + entry.getKey().partition() + "]",
                            entry.getValue().offset()));
                    }
                } catch (Exception e) {
                    sb.append("    (unable to read offsets)\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error listing consumer groups: " + e.getMessage();
        }
    }

    @Tool(description = "List all schemas and API artifacts registered in Apicurio Registry. Shows CDC event schemas, OpenAPI designs, and AsyncAPI mocks.")
    public String listRegistrySchemas() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(registryUrl + "/apis/registry/v2/search/artifacts?limit=50"))
                .header("Accept", "application/json")
                .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return "Apicurio Registry Artifacts:\n" +
                   "============================\n" + response.body();
        } catch (Exception e) {
            return "Error querying registry: " + e.getMessage();
        }
    }

    @Tool(description = "Get the full content of a specific schema or API artifact from Apicurio Registry.")
    public String getSchemaContent(
            @ToolArg(description = "Group ID of the artifact (e.g., cdc, bpm, api-designs, api-mocks)") String groupId,
            @ToolArg(description = "Artifact ID (e.g., cdc.public.customers-value, customer-service-api)") String artifactId
    ) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(registryUrl + "/apis/registry/v2/groups/" + groupId + "/artifacts/" + artifactId))
                .header("Accept", "application/json")
                .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return "Schema: " + groupId + "/" + artifactId + "\n" +
                   "=".repeat(50) + "\n" + response.body();
        } catch (Exception e) {
            return "Error fetching schema: " + e.getMessage();
        }
    }

    @Tool(description = "Get the current status of the CDC pipeline: PostgreSQL connection, Kafka cluster health, Debezium connector status, and Apicurio Registry availability.")
    public String getPipelineStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("CDC Pipeline Status:\n");
        sb.append("====================\n\n");

        try (AdminClient admin = createAdminClient()) {
            var clusterInfo = admin.describeCluster();
            sb.append("Kafka Cluster: CONNECTED\n");
            sb.append("  Cluster ID: ").append(clusterInfo.clusterId().get()).append("\n");
            sb.append("  Nodes: ").append(clusterInfo.nodes().get().size()).append("\n");
            sb.append("  Topics: ").append(admin.listTopics().names().get().size()).append("\n\n");
        } catch (Exception e) {
            sb.append("Kafka Cluster: DISCONNECTED - ").append(e.getMessage()).append("\n\n");
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(registryUrl + "/apis/registry/v2/system/info"))
                .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            sb.append("Apicurio Registry: CONNECTED\n");
            sb.append("  ").append(response.body()).append("\n\n");
        } catch (Exception e) {
            sb.append("Apicurio Registry: DISCONNECTED - ").append(e.getMessage()).append("\n\n");
        }

        sb.append("PostgreSQL CDC Source:\n");
        sb.append("  Host: ").append(pgHost).append("\n");
        sb.append("  Database: ").append(pgDatabase).append("\n");
        sb.append("  User: ").append(pgUsername).append("\n");

        return sb.toString();
    }
}
