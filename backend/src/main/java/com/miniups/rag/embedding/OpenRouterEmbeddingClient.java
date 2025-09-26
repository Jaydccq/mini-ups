package com.miniups.rag.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miniups.rag.config.RagProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@ConditionalOnProperty(name = "rag.embedding.provider", havingValue = "openrouter")
public class OpenRouterEmbeddingClient implements RagEmbeddingClient {

    private final RagProperties properties;
    private final RestTemplate restTemplate;

    public OpenRouterEmbeddingClient(RagProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder
            .rootUri(properties.getProviders().getOpenrouter().getBaseUrl())
            .setConnectTimeout(Duration.ofSeconds(15))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }

    @Override
    public List<float[]> embed(List<String> inputs) {
        if (CollectionUtils.isEmpty(inputs)) {
            return List.of();
        }

        RagProperties.Providers.OpenRouter config = properties.getProviders().getOpenrouter();
        if (!StringUtils.hasText(config.getApiKey())) {
            throw new IllegalStateException("OpenRouter API key is not configured for embeddings");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());
            headers.set("HTTP-Referer", config.getSiteUrl());
            headers.set("X-Title", config.getAppName());

            OpenRouterEmbeddingRequest payload = new OpenRouterEmbeddingRequest(
                properties.getEmbedding().getModel(),
                inputs
            );

            ResponseEntity<OpenRouterEmbeddingResponse> response = restTemplate.exchange(
                "/embeddings",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                OpenRouterEmbeddingResponse.class
            );

            OpenRouterEmbeddingResponse body = response.getBody();
            if (body == null || CollectionUtils.isEmpty(body.data())) {
                throw new IllegalStateException("OpenRouter embedding response is empty");
            }

            List<float[]> vectors = new ArrayList<>();
            for (OpenRouterEmbeddingResponse.Item item : body.data()) {
                List<Double> source = item.embedding();
                if (CollectionUtils.isEmpty(source)) {
                    continue;
                }
                float[] vector = new float[source.size()];
                for (int i = 0; i < source.size(); i++) {
                    vector[i] = source.get(i).floatValue();
                }
                vectors.add(vector);
            }
            return vectors;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to request embeddings from OpenRouter", ex);
        }
    }

    @Override
    public int embeddingDimensions() {
        return properties.getEmbedding().getDimensions();
    }

    private record OpenRouterEmbeddingRequest(String model, List<String> input) {
    }

    private record OpenRouterEmbeddingResponse(List<Item> data) {

        private record Item(@JsonProperty("embedding") List<Double> embedding) {
        }
    }
}
