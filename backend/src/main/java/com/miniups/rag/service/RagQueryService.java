package com.miniups.rag.service;

import com.miniups.model.entity.User;
import com.miniups.model.enums.UserRole;
import com.miniups.rag.api.RagQueryRequest;
import com.miniups.rag.api.RagQueryResponse;
import com.miniups.rag.api.RagSourceDto;
import com.miniups.rag.config.RagProperties;
import com.miniups.rag.embedding.RagEmbeddingClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.rag.generation.RagChatClient;
import com.miniups.rag.model.RagQueryLog;
import com.miniups.rag.repository.RagQueryLogRepository;
import com.miniups.rag.retrieval.RagRetriever;
import com.miniups.rag.retrieval.RagSearchResult;
import com.miniups.rag.security.RagRateLimiter;
import com.miniups.security.CustomUserDetailsService;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagQueryService {


    private static final Logger log = LoggerFactory.getLogger(RagQueryService.class);
    private static final String SYSTEM_PROMPT = "You are Mini-UPS operations assistant. " +
        "Provide concise, actionable answers grounded in the provided context. " +
        "Cite sources in-line using [n] markers matching the returned references. " +
        "If information is missing, state that explicitly and suggest next steps.";

    private final RagProperties properties;
    private final RagEmbeddingClient embeddingClient;
    private final RagRetriever retriever;
    private final RagChatClient chatClient;
    private final RagRateLimiter rateLimiter;
    private final RagQueryLogRepository queryLogRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public RagQueryResponse handleQuery(RagQueryRequest request, Authentication authentication) {
        Timer.Sample sample = Timer.start(meterRegistry);
        RagUserContext userContext = resolveUser(authentication);
        try {
            enforceRateLimit(userContext);

            String query = request.getQuery();
            if (!StringUtils.hasText(query)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query must not be blank");
            }

            List<float[]> vectors = embeddingClient.embed(List.of(query));
            if (CollectionUtils.isEmpty(vectors)) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate query embedding");
            }
            float[] queryVector = vectors.get(0);

            int topK = Math.max(1, properties.getRetrieval().getTopK());
            double threshold = properties.getRetrieval().getSimilarityThreshold();
            List<RagSearchResult> searchResults = retriever.hybridSearch(query, queryVector, topK, threshold);

            if (searchResults.isEmpty()) {
                meterRegistry.counter(
                    "rag.retrieval.empty_result",
                    "role",
                    userContext.role().name().toLowerCase(Locale.ROOT)
                ).increment();
                sample.stop(meterRegistry.timer(
                    "rag.retrieval.query_latency",
                    "role",
                    userContext.role().name().toLowerCase(Locale.ROOT),
                    "status",
                    "empty"
                ));
                return new RagQueryResponse(
                    null,  // logId
                    "未能在当前知识库中找到相关信息，请联系管理员或查阅手册。",  // answer
                    0.0,  // confidence
                    List.of(),  // sources
                    List.of("未检索到相关内容")  // warnings
                );
            }

            String prompt = buildUserPrompt(query, searchResults, userContext);
            String answer = chatClient.generate(
                SYSTEM_PROMPT,
                prompt,
                properties.getLlm().getTemperature(),
                properties.getLlm().getMaxOutputTokens()
            );

            double confidence = computeConfidence(searchResults);
            List<RagSourceDto> sources = mapSources(searchResults);

            recordRetrievalMetrics(userContext, searchResults);

            UUID logId = persistQueryLog(userContext, query, answer, confidence, sources);

            RagQueryResponse response = new RagQueryResponse(
                logId,
                answer,
                confidence,
                sources,
                null  // warnings
            );

            sample.stop(meterRegistry.timer(
                "rag.retrieval.query_latency",
                "role",
                userContext.role().name().toLowerCase(Locale.ROOT),
                "status",
                "success"
            ));

            return response;
        } catch (RuntimeException ex) {
            sample.stop(meterRegistry.timer(
                "rag.retrieval.query_latency",
                "role",
                userContext.role().name().toLowerCase(Locale.ROOT),
                "status",
                "error"
            ));
            throw ex;
        }
    }

    private void enforceRateLimit(RagUserContext context) {
        int limit = switch (context.role()) {
            case ADMIN -> properties.getRateLimit().getAdmin();
            case DRIVER -> properties.getRateLimit().getDriver();
            case OPERATOR -> properties.getRateLimit().getDispatcher();
            default -> properties.getRateLimit().getDispatcher();
        };
        if (!rateLimiter.tryConsume(context.userId(), limit)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "RAG query rate limit exceeded");
        }
    }

    private RagUserContext resolveUser(Authentication authentication) {
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetailsService.CustomUserPrincipal custom) {
            User user = custom.getUser();
            UserRole role = user.getRole() != null ? user.getRole() : UserRole.USER;
            return new RagUserContext(String.valueOf(user.getId()), user.getUsername(), role, auth.getAuthorities());
        }
        return new RagUserContext(auth.getName(), auth.getName(), resolveRole(auth), auth.getAuthorities());
    }

    private UserRole resolveRole(Authentication auth) {
        if (auth == null) {
            return UserRole.USER;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String authName = authority.getAuthority();
            if ("ROLE_ADMIN".equalsIgnoreCase(authName)) {
                return UserRole.ADMIN;
            }
            if ("ROLE_DRIVER".equalsIgnoreCase(authName)) {
                return UserRole.DRIVER;
            }
            if ("ROLE_OPERATOR".equalsIgnoreCase(authName) || "ROLE_DISPATCHER".equalsIgnoreCase(authName)) {
                return UserRole.OPERATOR;
            }
        }
        return UserRole.USER;
    }

    private String buildUserPrompt(String query, List<RagSearchResult> results, RagUserContext context) {
        StringBuilder builder = new StringBuilder();
        builder.append("问题:\n").append(query.trim()).append("\n\n");
        builder.append("用户角色: ").append(context.role().name().toLowerCase(Locale.ROOT)).append("\n");
        builder.append("上下文片段:\n");
        int index = 1;
        for (RagSearchResult result : results) {
            builder.append("[片段 ").append(index).append("] 来源: ")
                .append(result.source()).append("\n")
                .append(result.content()).append("\n\n");
            index++;
        }
        builder.append("请基于以上内容回答用户问题，并在答案中插入对应的引用标记 (例如 [1])。");
        return builder.toString();
    }

    private double computeConfidence(List<RagSearchResult> results) {
        double max = 0.0;
        for (RagSearchResult result : results) {
            max = Math.max(max, result.finalScore());
        }
        return Math.min(1.0, Math.max(0.0, max));
    }

    private List<RagSourceDto> mapSources(List<RagSearchResult> results) {
        List<RagSourceDto> sources = new ArrayList<>();
        int index = 1;
        for (RagSearchResult result : results) {
            String title = Objects.toString(result.metadata().getOrDefault("title", result.source()), result.source());
            sources.add(new RagSourceDto(
                title,
                result.source(),
                result.finalScore(),  // similarity
                result.finalScore(),  // confidence
                result.semanticScore(),
                result.keywordScore()
            ));
            index++;
        }
        return sources;
    }

    private void recordRetrievalMetrics(RagUserContext context, List<RagSearchResult> results) {
        String roleTag = context.role().name().toLowerCase(Locale.ROOT);

        DistributionSummary semanticSummary = DistributionSummary.builder("rag.retrieval.semantic_score")
            .description("Semantic similarity distribution for retrieved chunks")
            .baseUnit("ratio")
            .tags("role", roleTag)
            .register(meterRegistry);

        DistributionSummary keywordSummary = DistributionSummary.builder("rag.retrieval.keyword_score")
            .description("Keyword relevance distribution for retrieved chunks")
            .baseUnit("ratio")
            .tags("role", roleTag)
            .register(meterRegistry);

        DistributionSummary finalSummary = DistributionSummary.builder("rag.retrieval.final_score")
            .description("Final weighted score distribution for retrieved chunks")
            .baseUnit("ratio")
            .tags("role", roleTag)
            .register(meterRegistry);

        double semanticWeight = results.stream().mapToDouble(RagSearchResult::semanticScore).sum();
        double keywordWeight = results.stream().mapToDouble(RagSearchResult::keywordScore).sum();
        String dominant = semanticWeight >= keywordWeight ? "semantic" : "keyword";
        meterRegistry.counter(
            "rag.retrieval.weight_dominant",
            "type",
            dominant,
            "role",
            roleTag
        ).increment();

        for (RagSearchResult result : results) {
            semanticSummary.record(result.semanticScore());
            keywordSummary.record(result.keywordScore());
            finalSummary.record(result.finalScore());
        }
    }

    private UUID persistQueryLog(
        RagUserContext context,
        String query,
        String answer,
        double confidence,
        List<RagSourceDto> sources
    ) {
        try {
            RagQueryLog logEntry = new RagQueryLog();
            logEntry.setUserId(context.userId());
            logEntry.setUsername(context.username());
            logEntry.setRole(context.role().name());
            logEntry.setQuery(query);
            logEntry.setAnswer(answer);
            logEntry.setConfidence(confidence);
            if (sources != null && !sources.isEmpty()) {
                logEntry.setSources(objectMapper.writeValueAsString(sources));
            }
            queryLogRepository.insert(logEntry);
            return logEntry.getId();
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize RAG sources for logging", ex);
        } catch (Exception ex) {
            log.warn("Failed to persist RAG query log", ex);
        }
        return null;
    }

    private record RagUserContext(String userId, String username, UserRole role, java.util.Collection<? extends GrantedAuthority> authorities) {
    }
}
