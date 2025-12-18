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
import com.miniups.rag.model.OrderSummary;
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
    private static final String SYSTEM_PROMPT = """
        你是 Mini-UPS 智能物流助手。你的职责是帮助用户解决订单和物流相关问题。
        
        回答原则：
        1. 如果用户询问具体订单，优先使用【用户订单信息】部分的数据
        2. 结合知识库内容给出完整的解决方案
        3. 回答要具体、可操作，包含明确的下一步行动
        4. 引用知识库来源时使用 [n] 标记
        
        回答格式：
        - 先简要回应用户问题
        - 给出具体解决步骤或建议
        - 如有相关订单，引用具体信息（追踪号、状态等）
        - 结尾可提供进一步帮助的建议
        
        如果信息不足以回答，明确说明并建议联系客服。
        """;

    private final RagProperties properties;
    private final RagEmbeddingClient embeddingClient;
    private final RagRetriever retriever;
    private final RagChatClient chatClient;
    private final RagRateLimiter rateLimiter;
    private final RagQueryLogRepository queryLogRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final OrderContextProvider orderContextProvider;

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

            // Fetch order context if query is order-related
            String orderContext = "";
            if (orderContextProvider.isOrderRelatedQuery(query)) {
                try {
                    Long userId = Long.parseLong(userContext.userId());
                    List<OrderSummary> orders = orderContextProvider.getUserOrderContext(userId, 5);
                    orderContext = orderContextProvider.formatOrderContext(orders);
                    log.debug("Injected order context for user {}: {} orders", userId, orders.size());
                } catch (NumberFormatException e) {
                    log.debug("Could not parse userId for order context: {}", userContext.userId());
                }
            }

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

            String prompt = buildUserPrompt(query, searchResults, userContext, orderContext);
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

    private String buildUserPrompt(String query, List<RagSearchResult> results, 
                                     RagUserContext context, String orderContext) {
        StringBuilder builder = new StringBuilder();
        
        // Add order context if available
        if (StringUtils.hasText(orderContext)) {
            builder.append("【用户订单信息】\n").append(orderContext).append("\n");
        }
        
        builder.append("【用户问题】\n").append(query.trim()).append("\n\n");
        builder.append("用户角色: ").append(context.role().name().toLowerCase(Locale.ROOT)).append("\n\n");
        
        if (!results.isEmpty()) {
            builder.append("【知识库参考内容】\n");
            int index = 1;
            for (RagSearchResult result : results) {
                builder.append("[片段 ").append(index).append("] 来源: ")
                    .append(result.source()).append("\n")
                    .append(result.content()).append("\n\n");
                index++;
            }
        }
        
        builder.append("请基于以上信息回答用户问题。如有订单数据请引用，如有知识库内容请使用 [n] 标记引用。");
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
