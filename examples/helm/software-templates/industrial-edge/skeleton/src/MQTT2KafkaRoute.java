// dependency=camel:camel-endpointdsl
// dependency=camel:kafka
package com.redhat.industrialedge.routes;

import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.model.OnCompletionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQTT2KafkaRoute extends RouteBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MQTT2KafkaRoute.class);

    @PropertyInject("kafka.broker.uri")
    private String kafka_broker_uri;
    @PropertyInject("kafka.broker.topic.temperature")
    private String kafka_broker_topic_temperature;
    @PropertyInject("kafka.broker.topic.vibration")
    private String kafka_broker_topic_vibration;

    @PropertyInject("mqtt.broker.uri")
    private String mqtt_broker_uri;
    @PropertyInject("mqtt.broker.clientId")
    private String mqtt_broker_clientid;
    @PropertyInject("mqtt.broker.topic.temperature")
    private String mqtt_broker_topic_temperature;
    @PropertyInject("mqtt.broker.topic.vibration")
    private String mqtt_broker_topic_vibration;

    @Override
    public void configure() throws Exception {
        storeTemperatureInKafka();
        storeVibrationInKafka();
    }

    private void storeTemperatureInKafka() {
        from("paho:" + mqtt_broker_topic_temperature
                + "?brokerUrl=" + mqtt_broker_uri
                + "&clientId=" + mqtt_broker_clientid + "-temp")
            .log("Storing temperature message MQTT → Kafka: ${body}")
            .setHeader(KafkaConstants.KEY, constant("${{values.uniqueName}}"))
            .to("kafka:" + kafka_broker_topic_temperature + "?brokers=" + kafka_broker_uri);
    }

    private void storeVibrationInKafka() {
        from("paho:" + mqtt_broker_topic_vibration
                + "?brokerUrl=" + mqtt_broker_uri
                + "&clientId=" + mqtt_broker_clientid + "-vibr")
            .log("Storing vibration message MQTT → Kafka: ${body}")
            .setHeader(KafkaConstants.KEY, constant("${{values.uniqueName}}"))
            .to("kafka:" + kafka_broker_topic_vibration + "?brokers=" + kafka_broker_uri);
    }

    @Override
    public OnCompletionDefinition onCompletion() {
        return super.onCompletion();
    }
}
