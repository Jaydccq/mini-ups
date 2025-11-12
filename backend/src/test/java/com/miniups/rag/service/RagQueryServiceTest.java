package com.miniups.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.rag.api.RagQueryRequest;
import com.miniups.rag.api.RagQueryResponse;
import com.miniups.rag.api.RagSourceDto;
import com.miniups.rag.config.RagProperties;
import com.miniups.rag.embedding.RagEmbeddingClient;
import com.miniups.rag.generation.RagChatClient;
import com.miniups.rag.model.RagQueryLog;
import com.miniups.rag.repository.RagQueryLogRepository;
import com.miniups.rag.retrieval.RagRetriever;
import com.miniups.rag.retrieval.RagSearchResult;
import com.miniups.rag.security.RagRateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RAG Query Service Tests")
class RagQueryServiceTest {

    @Mock
    private RagEmbeddingClient embeddingClient;

    @Mock
    private RagRetriever retriever;

    @Mock
    private RagChatClient chatClient;

    @Mock
    private RagRateLimiter rateLimiter;

    @Mock
    private RagQueryLogRepository queryLogRepository;

    private RagProperties properties;
    private MeterRegistry meterRegistry;
    private RagQueryService queryService;

    @BeforeEach
    void setUp() {
        properties = new RagProperties();
        properties.getRetrieval().setTopK(5);
        properties.getRetrieval().setSimilarityThreshold(0.7);
        properties.getLlm().setTemperature(0.3);
        properties.getLlm().setMaxOutputTokens(600);

        meterRegistry = new SimpleMeterRegistry();

        when(rateLimiter.tryConsume(anyString(), anyInt())).thenReturn(true);
        when(queryLogRepository.insert(any())).thenAnswer(invocation -> {
            RagQueryLog log = invocation.getArgument(0);
            if (log.getId() == null) {
                log.setId(UUID.randomUUID());
            }
            return 1; // MyBatis insert returns int (number of rows affected)
        });

        queryService = new RagQueryService(
            properties,
            embeddingClient,
            retriever,
            chatClient,
            rateLimiter,
            queryLogRepository,
            new ObjectMapper(),
            meterRegistry
        );
    }

    private TestingAuthenticationToken authenticatedUser() {
        return new TestingAuthenticationToken("test-user", null, "ROLE_ADMIN");
    }

    @Nested
    @DisplayName("Query Processing")
    class QueryProcessingTests {

        @Test
        @DisplayName("Should process query and return response with sources")
        void shouldProcessQueryAndReturnResponseWithSources() {
            RagQueryRequest request = new RagQueryRequest();
            request.setQuery("How to handle delayed delivery?");

            float[] queryVector = new float[] {0.1f, 0.2f, 0.3f};
            when(embeddingClient.embed(anyList())).thenReturn(List.of(queryVector));

            RagSearchResult searchResult = new RagSearchResult(
                UUID.randomUUID(),
                "doc-1",
                "knowledge/delivery.md",
                0,
                "延迟配送处理流程：联系司机确认位置，通知客户状态，更新系统记录",
                0.85,
                0.75,
                0.82,
                Map.of("title", "配送手册", "section", "延迟处理")
            );
            when(retriever.hybridSearch(anyString(), any(float[].class), anyInt(), anyDouble()))
                .thenReturn(List.of(searchResult));

            String llmResponse = "For delayed deliveries, contact the driver, notify the customer, and update the system.";
            when(chatClient.generate(anyString(), anyString(), anyDouble(), anyInt())).thenReturn(llmResponse);

            RagQueryResponse response = queryService.handleQuery(request, authenticatedUser());

            assertThat(response).isNotNull();
            assertThat(response.getAnswer()).isEqualTo(llmResponse);
            assertThat(response.getSources()).hasSize(1);

            RagSourceDto source = response.getSources().get(0);
            assertThat(source.getTitle()).isEqualTo("配送手册");
            assertThat(source.getSource()).isEqualTo("knowledge/delivery.md");
            assertThat(source.getConfidence()).isEqualTo(0.82);
            assertThat(source.getSemanticScore()).isEqualTo(0.85);
            assertThat(source.getKeywordScore()).isEqualTo(0.75);
            assertThat(response.getConfidence()).isEqualTo(0.82);
            assertThat(response.getLogId()).isNotNull();
        }

        @Test
        @DisplayName("Should return fallback when no search results found")
        void shouldReturnFallbackWhenNoResults() {
            RagQueryRequest request = new RagQueryRequest();
            request.setQuery("Completely unrelated query");

            when(embeddingClient.embed(anyList())).thenReturn(List.of(new float[] {0.1f, 0.2f, 0.3f}));
            when(retriever.hybridSearch(anyString(), any(float[].class), anyInt(), anyDouble()))
                .thenReturn(List.of());

            RagQueryResponse response = queryService.handleQuery(request, authenticatedUser());

            assertThat(response.getSources()).isEmpty();
            assertThat(response.getAnswer()).contains("未能在当前知识库中找到相关信息");
            verify(chatClient, never()).generate(anyString(), anyString(), anyDouble(), anyInt());
        }

        @Test
        @DisplayName("Should surface embedding client failures")
        void shouldSurfaceEmbeddingFailures() {
            RagQueryRequest request = new RagQueryRequest();
            request.setQuery("Test query");

            when(embeddingClient.embed(anyList())).thenThrow(new RuntimeException("Embedding service unavailable"));

            assertThrows(RuntimeException.class, () -> queryService.handleQuery(request, authenticatedUser()));
        }

        @Test
        @DisplayName("Should surface chat client failures")
        void shouldSurfaceChatClientFailures() {
            RagQueryRequest request = new RagQueryRequest();
            request.setQuery("Test query");

            float[] queryVector = new float[] {0.1f, 0.2f, 0.3f};
            when(embeddingClient.embed(anyList())).thenReturn(List.of(queryVector));

            RagSearchResult searchResult = new RagSearchResult(
                UUID.randomUUID(), "doc-1", "source.md", 0, "content",
                0.8, 0.6, 0.75, Map.of()
            );
            when(retriever.hybridSearch(anyString(), any(float[].class), anyInt(), anyDouble()))
                .thenReturn(List.of(searchResult));

            when(chatClient.generate(anyString(), anyString(), anyDouble(), anyInt()))
                .thenThrow(new RuntimeException("LLM service unavailable"));

            assertThrows(RuntimeException.class, () -> queryService.handleQuery(request, authenticatedUser()));
        }

        @Test
        @DisplayName("Should reject blank queries")
        void shouldRejectBlankQueries() {
            RagQueryRequest request = new RagQueryRequest();
            request.setQuery(" ");

            assertThrows(ResponseStatusException.class, () -> queryService.handleQuery(request, authenticatedUser()));
        }
    }
}
