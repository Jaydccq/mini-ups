package com.miniups.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Kafka-based messaging integration.
 */
@Data
@ConfigurationProperties(prefix = "messaging.kafka")
public class KafkaMessagingProperties {

    /**
     * Master switch for enabling Kafka infrastructure.
     */
    private boolean enabled = false;

    /**
     * Whether the outbox publisher should send events to Kafka.
     */
    private boolean outboxEnabled = false;

    /**
     * Optional prefix applied to all topic names used by the application.
     */
    private String topicPrefix = "";

    /**
     * Fallback topic used when an outbox event does not specify a routing key.
     */
    private String defaultTopic;

    /**
     * Timeout (milliseconds) to wait for broker acknowledgement when publishing an event.
     */
    private long sendTimeoutMs = 5000;

    /**
     * Automatically create application topics on startup.
     */
    private boolean autoCreateTopics = true;

    /**
     * Topics that should be created when {@link #autoCreateTopics} is enabled.
     */
    private List<TopicDefinition> topics = new ArrayList<>();

    // Manual getters
    public List<TopicDefinition> getTopics() { return topics; }
    public String getTopicPrefix() { return topicPrefix; }
    public String getDefaultTopic() { return defaultTopic; }
    public long getSendTimeoutMs() { return sendTimeoutMs; }

    @Data
    public static class TopicDefinition {
        private String name;
        private Integer partitions = 3;
        private Short replicationFactor = 1;

        // Manual getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getPartitions() { return partitions; }
        public void setPartitions(Integer partitions) { this.partitions = partitions; }
        public Short getReplicationFactor() { return replicationFactor; }
        public void setReplicationFactor(Short replicationFactor) { this.replicationFactor = replicationFactor; }
    }
}
