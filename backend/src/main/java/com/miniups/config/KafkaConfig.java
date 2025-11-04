package com.miniups.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.miniups.config.RabbitMQConfig.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Kafka infrastructure configuration responsible for topic management and enabling {@code @KafkaListener} support.
 */
@Configuration
@EnableKafka
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "messaging.kafka", name = "enabled", havingValue = "true")
public class KafkaConfig {


    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);
    private final KafkaMessagingProperties messagingProperties;

    // Manual constructor (Lombok @RequiredArgsConstructor not working)
    public KafkaConfig(KafkaMessagingProperties messagingProperties) {
        this.messagingProperties = messagingProperties;
    }

    @Bean
    @ConditionalOnBean(KafkaAdmin.class)
    @ConditionalOnProperty(prefix = "messaging.kafka", name = "auto-create-topics", havingValue = "true", matchIfMissing = true)
    public KafkaAdmin.NewTopics kafkaTopics() {
        List<KafkaMessagingProperties.TopicDefinition> topicDefinitions = messagingProperties.getTopics();
        if (CollectionUtils.isEmpty(topicDefinitions)) {
            topicDefinitions = defaultTopics();
        }

        List<NewTopic> topics = topicDefinitions.stream()
                .filter(def -> StringUtils.hasText(def.getName()))
                .map(def -> TopicBuilder
                        .name(applyPrefix(def.getName()))
                        .partitions(def.getPartitions() != null ? def.getPartitions() : 3)
                        .replicas(def.getReplicationFactor() != null ? def.getReplicationFactor() : 1)
                        .build())
                .collect(Collectors.toList());

        if (topics.isEmpty()) {
            log.warn("Kafka topic auto-creation requested but no topics were resolved");
            return new KafkaAdmin.NewTopics(new NewTopic[0]);
        }

        if (log.isInfoEnabled()) {
            log.info("Kafka auto-creation enabled for topics: {}",
                    topics.stream().map(NewTopic::name).collect(Collectors.joining(", ")));
        }

        return new KafkaAdmin.NewTopics(topics.toArray(NewTopic[]::new));
    }

    private List<KafkaMessagingProperties.TopicDefinition> defaultTopics() {
        List<KafkaMessagingProperties.TopicDefinition> defaults = new ArrayList<>();
        defaults.add(topic(RabbitMQConfig.SHIPMENT_CREATE_ROUTING_KEY, 6, 1));
        defaults.add(topic(RabbitMQConfig.SHIPMENT_STATUS_ROUTING_KEY, 6, 1));
        defaults.add(topic(RabbitMQConfig.USER_REGISTERED_ROUTING_KEY, 3, 1));
        defaults.add(topic(RabbitMQConfig.TRUCK_DISPATCH_ROUTING_KEY, 3, 1));
        defaults.add(topic(RabbitMQConfig.AUDIT_LOG_ROUTING_KEY, 3, 1));
        return defaults;
    }

    private KafkaMessagingProperties.TopicDefinition topic(String name, int partitions, int replicationFactor) {
        KafkaMessagingProperties.TopicDefinition definition = new KafkaMessagingProperties.TopicDefinition();
        definition.setName(name);
        definition.setPartitions(partitions);
        definition.setReplicationFactor((short) replicationFactor);
        return definition;
    }

    private String applyPrefix(String name) {
        String prefix = messagingProperties.getTopicPrefix();
        if (StringUtils.hasText(prefix)) {
            return prefix + name;
        }
        return name;
    }
}
