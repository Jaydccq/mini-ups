package com.miniups.rag.service;

import com.miniups.rag.api.RagQueryRequest;
import com.miniups.rag.api.RagQueryResponse;
import com.miniups.rag.api.RagSourceDto;
import com.miniups.rag.config.RagProperties;
import com.miniups.rag.retrieval.RagRetriever;
import com.miniups.rag.retrieval.RagSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RAG Query Service Tests")
class RagQueryServiceTest {

    @Mock
    private RagRetriever retriever;

    @Mock
    private RagEmbeddingService embeddingService;

    @Mock
    private RagLlmService llmService;

    private RagProperties properties;
    private RagQueryService queryService;

    @BeforeEach
    void setUp() {
        properties = new RagProperties();
        properties.getRetrieval().setTopK(5);
        properties.getRetrieval().setSimilarityThreshold(0.7);

        queryService = new RagQueryService(retriever, embeddingService, llmService, properties);
    }

    @Nested
    @DisplayName("Query Processing Tests")
    class QueryProcessingTests {

        @Test
        @DisplayName("Should process query and return response with sources")
        void shouldProcessQueryAndReturnResponseWithSources() {
            // Given
            String query = "How to handle delayed delivery?";
            RagQueryRequest request = new RagQueryRequest(query, null);

            float[] queryVector = {0.1f, 0.2f, 0.3f};
            when(embeddingService.embed(query)).thenReturn(queryVector);

            UUID chunkId = UUID.randomUUID();
            RagSearchResult searchResult = new RagSearchResult(
                chunkId,
                "doc-1",
                "knowledge/delivery.md",
                0,
                "延迟配送处理流程：联系司机确认位置，通知客户状态，更新系统记录",
                0.85,
                0.75,
                0.82,
                Map.of("title", "配送手册", "section", "延迟处理")
            );

            when(retriever.hybridSearch(eq(query), eq(queryVector), eq(5), eq(0.7)))
                .thenReturn(List.of(searchResult));

            String llmResponse = "For delayed deliveries, you should: 1. Contact the driver to confirm location...";
            when(llmService.generateResponse(eq(query), eq(List.of(searchResult))))
                .thenReturn(llmResponse);

            // When
            RagQueryResponse response = queryService.query(request);

            // Then
            assertNotNull(response);
            assertEquals(llmResponse, response.getAnswer());
            assertFalse(response.getSources().isEmpty());

            RagSourceDto source = response.getSources().get(0);
            assertEquals("配送手册 - 延迟处理", source.getTitle());
            assertEquals("knowledge/delivery.md", source.getSource());
            assertEquals(0.82, source.getConfidence(), 0.001);
            assertEquals(0.85, source.getSemanticScore(), 0.001);
            assertEquals(0.75, source.getKeywordScore(), 0.001);
        }

        @Test
        @DisplayName("Should handle query with no search results")
        void shouldHandleQueryWithNoSearchResults() {
            // Given
            String query = "Completely unrelated query";
            RagQueryRequest request = new RagQueryRequest(query, null);

            float[] queryVector = {0.1f, 0.2f, 0.3f};
            when(embeddingService.embed(query)).thenReturn(queryVector);
            when(retriever.hybridSearch(eq(query), eq(queryVector), eq(5), eq(0.7)))
                .thenReturn(List.of());

            String fallbackResponse = "I don't have specific information about that topic.";
            when(llmService.generateResponse(eq(query), eq(List.of())))
                .thenReturn(fallbackResponse);

            // When
            RagQueryResponse response = queryService.query(request);

            // Then
            assertNotNull(response);
            assertEquals(fallbackResponse, response.getAnswer());
            assertTrue(response.getSources().isEmpty());
        }

        @Test
        @DisplayName("Should handle embedding service failure")
        void shouldHandleEmbeddingServiceFailure() {
            // Given
            String query = "Test query";
            RagQueryRequest request = new RagQueryRequest(query, null);

            when(embeddingService.embed(query)).thenThrow(new RuntimeException("Embedding service unavailable"));

            // When & Then
            assertThrows(RuntimeException.class, () -> queryService.query(request));
        }

        @Test
        @DisplayName("Should handle LLM service failure gracefully")
        void shouldHandleLlmServiceFailureGracefully() {
            // Given
            String query = "Test query";
            RagQueryRequest request = new RagQueryRequest(query, null);

            float[] queryVector = {0.1f, 0.2f, 0.3f};
            when(embeddingService.embed(query)).thenReturn(queryVector);

            RagSearchResult searchResult = new RagSearchResult(
                UUID.randomUUID(), "doc-1", "source.md", 0, "content",
                0.8, 0.6, 0.75, Map.of()
            );
            when(retriever.hybridSearch(eq(query), eq(queryVector), eq(5), eq(0.7)))
                .thenReturn(List.of(searchResult));

            when(llmService.generateResponse(eq(query), eq(List.of(searchResult))))
                .thenThrow(new RuntimeException("LLM service unavailable"));

            // When & Then
            assertThrows(RuntimeException.class, () -> queryService.query(request));
        }
    }

    @Nested
    @DisplayName("Source Processing Tests")
    class SourceProcessingTests {

        @Test
        @DisplayName("Should create source DTO with complete metadata")
        void shouldCreateSourceDtoWithCompleteMetadata() {
            // Given
            String query = "Test query";
            RagQueryRequest request = new RagQueryRequest(query, null);

            float[] queryVector = {0.1f, 0.2f, 0.3f};
            when(embeddingService.embed(query)).thenReturn(queryVector);

            RagSearchResult searchResult = new RagSearchResult(
                UUID.randomUUID(),
                "doc-1",
                "knowledge/routes.md",
                2,
                "路线优化算法内容",
                0.9,
                0.8,
                0.87,
                Map.of(
                    "title", "路线规划手册",
                    "section", "优化算法",
                    "author", "系统管理员",
                    "version", "1.2"
                )
            );

            when(retriever.hybridSearch(eq(query), eq(queryVector), eq(5), eq(0.7)))
                .thenReturn(List.of(searchResult));

            when(llmService.generateResponse(any(), any())).thenReturn("Generated response");

            // When
            RagQueryResponse response = queryService.query(request);

            // Then
            assertFalse(response.getSources().isEmpty());
            RagSourceDto source = response.getSources().get(0);

            assertEquals("路线规划手册 - 优化算法", source.getTitle());
            assertEquals("knowledge/routes.md", source.getSource());
            assertEquals(0.87, source.getConfidence(), 0.001);
            assertEquals(0.9, source.getSemanticScore(), 0.001);
            assertEquals(0.8, source.getKeywordScore(), 0.001);
        }

        @Test
        @DisplayName("Should handle source with minimal metadata")
        void shouldHandleSourceWithMinimalMetadata() {
            // Given
            String query = "Test query";
            RagQueryRequest request = new RagQueryRequest(query, null);

            float[] queryVector = {0.1f, 0.2f, 0.3f};
            when(embeddingService.embed(query)).thenReturn(queryVector);

            RagSearchResult searchResult = new RagSearchResult(
                UUID.randomUUID(),
                "doc-1",
                "simple.txt",
                0,
                "Simple content",
                0.7,
                0.0,
                0.49, // 0.7 * 0.7 + 0.0 * 0.3 = 0.49
                Map.of() // Empty metadata
            );

            when(retriever.hybridSearch(eq(query), eq(queryVector), eq(5), eq(0.7)))
                .thenReturn(List.of(searchResult));

            when(llmService.generateResponse(any(), any())).thenReturn("Generated response");

            // When
            RagQueryResponse response = queryService.query(request);

            // Then
            assertFalse(response.getSources().isEmpty());
            RagSourceDto source = response.getSources().get(0);

            assertEquals("simple.txt", source.getTitle()); // Should fallback to source name
            assertEquals("simple.txt", source.getSource());
            assertEquals(0.49, source.getConfidence(), 0.001);
        }

        @Test
        @DisplayName("Should handle multiple sources with different metadata structures")
        void shouldHandleMultipleSourcesWithDifferentMetadata() {
            // Given
            String query = "Test query";
            RagQueryRequest request = new RagQueryRequest(query, null);

            float[] queryVector = {0.1f, 0.2f, 0.3f};
            when(embeddingService.embed(query)).thenReturn(queryVector);

            List<RagSearchResult> searchResults = List.of(
                new RagSearchResult(
                    UUID.randomUUID(), "doc-1", "manual.md", 0, "content1",
                    0.9, 0.8, 0.87, Map.of("title", "手册", "section", "第一章")
                ),
                new RagSearchResult(
                    UUID.randomUUID(), "doc-2", "guide.txt", 1, "content2",
                    0.8, 0.6, 0.74, Map.of("title", "指南") // No section
                ),
                new RagSearchResult(
                    UUID.randomUUID(), "doc-3", "notes.md", 0, "content3",
                    0.7, 0.0, 0.49, Map.of() // Empty metadata
                )
            );

            when(retriever.hybridSearch(eq(query), eq(queryVector), eq(5), eq(0.7)))
                .thenReturn(searchResults);

            when(llmService.generateResponse(any(), any())).thenReturn("Generated response");

            // When
            RagQueryResponse response = queryService.query(request);

            // Then
            assertEquals(3, response.getSources().size());

            RagSourceDto source1 = response.getSources().get(0);
            assertEquals("手册 - 第一章", source1.getTitle());
            assertEquals(0.87, source1.getConfidence(), 0.001);

            RagSourceDto source2 = response.getSources().get(1);
            assertEquals("指南", source2.getTitle()); // No section, just title
            assertEquals(0.74, source2.getConfidence(), 0.001);

            RagSourceDto source3 = response.getSources().get(2);
            assertEquals("notes.md", source3.getTitle()); // Fallback to source name
            assertEquals(0.49, source3.getConfidence(), 0.001);
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should use configured topK value")
        void shouldUseConfiguredTopKValue() {
            // Given
            properties.getRetrieval().setTopK(3);
            queryService = new RagQueryService(retriever, embeddingService, llmService, properties);

            String query = "Test query";
            RagQueryRequest request = new RagQueryRequest(query, null);

            float[] queryVector = {0.1f, 0.2f, 0.3f};
            when(embeddingService.embed(query)).thenReturn(queryVector);
            when(retriever.hybridSearch(eq(query), eq(queryVector), eq(3), eq(0.7)))
                .thenReturn(List.of());
            when(llmService.generateResponse(any(), any())).thenReturn("response");

            // When
            queryService.query(request);

            // Then
            verify(retriever).hybridSearch(eq(query), eq(queryVector), eq(3), eq(0.7));
        }

        @Test
        @DisplayName("Should use configured similarity threshold")
        void shouldUseConfiguredSimilarityThreshold() {
            // Given
            properties.getRetrieval().setSimilarityThreshold(0.8);
            queryService = new RagQueryService(retriever, embeddingService, llmService, properties);

            String query = "Test query";
            RagQueryRequest request = new RagQueryRequest(query, null);

            float[] queryVector = {0.1f, 0.2f, 0.3f};
            when(embeddingService.embed(query)).thenReturn(queryVector);
            when(retriever.hybridSearch(eq(query), eq(queryVector), eq(5), eq(0.8)))
                .thenReturn(List.of());
            when(llmService.generateResponse(any(), any())).thenReturn("response");

            // When
            queryService.query(request);

            // Then
            verify(retriever).hybridSearch(eq(query), eq(queryVector), eq(5), eq(0.8));
        }
    }
}