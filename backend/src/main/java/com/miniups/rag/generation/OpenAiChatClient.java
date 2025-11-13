package com.miniups.rag.generation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miniups.rag.config.RagProperties;
import java.time.Duration;
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
@ConditionalOnProperty(name = "rag.llm.provider", havingValue = "openai")
public class OpenAiChatClient implements RagChatClient {

    private final RagProperties properties;
    private final RestTemplate restTemplate;

    public OpenAiChatClient(RagProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder
            .rootUri(properties.getProviders().getOpenai().getBaseUrl())
            .setConnectTimeout(Duration.ofSeconds(15))
            .setReadTimeout(Duration.ofSeconds(60))
            .build();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt, double temperature, int maxOutputTokens) {
        RagProperties.Providers.OpenAi openAi = properties.getProviders().getOpenai();
        if (!StringUtils.hasText(openAi.getApiKey())) {
            throw new IllegalStateException("OpenAI API key is not configured for generation");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAi.getApiKey());

            OpenAiChatRequest request = new OpenAiChatRequest(
                properties.getLlm().getModel(),
                temperature,
                maxOutputTokens,
                List.of(
                    new Message("system", systemPrompt),
                    new Message("user", userPrompt)
                )
            );

            ResponseEntity<OpenAiChatResponse> response = restTemplate.exchange(
                "/chat/completions",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                OpenAiChatResponse.class
            );
            OpenAiChatResponse body = response.getBody();
            if (body == null || CollectionUtils.isEmpty(body.choices())) {
                throw new IllegalStateException("OpenAI chat completion returned empty response");
            }
            for (Choice choice : body.choices()) {
                if (choice.message() != null && StringUtils.hasText(choice.message().content())) {
                    return choice.message().content();
                }
            }
            throw new IllegalStateException("OpenAI chat completion did not include a message");
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to request completion from OpenAI", ex);
        }
    }

    private record OpenAiChatRequest(
        String model,
        double temperature,
        @JsonProperty("max_tokens") int maxTokens,
        List<Message> messages
    ) {
    }

    private record Message(String role, String content) {
    }

    private record Choice(Message message) {
    }

    private record OpenAiChatResponse(List<Choice> choices) {
    }
}
