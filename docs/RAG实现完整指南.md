# RAG系统实现完整指南

## 概述

本指南将从零开始，详细介绍如何在Spring Boot + React项目中实现一个完整的RAG（检索增强生成）系统。该系统使用PostgreSQL + pgvector进行向量存储，结合关键词搜索实现混合检索，最终通过LLM生成智能回答。

## 系统架构

```
用户查询 → 前端组件 → 后端API → 混合检索 → LLM生成 → 返回回答
                           ↓
                    [语义搜索] + [关键词搜索]
                           ↓
                    PostgreSQL + pgvector
```

## 第一部分：环境准备

### 1.1 依赖配置

#### 后端Maven依赖 (pom.xml)

```xml
<!-- pgvector支持 -->
<dependency>
    <groupId>com.pgvector</groupId>
    <artifactId>pgvector</artifactId>
    <version>0.1.6</version>
</dependency>

<!-- HTTP客户端用于调用LLM API -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- JSON处理 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- 数据库连接 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

### 1.2 Docker环境

#### docker-compose.yml配置

```yaml
# UPS PostgreSQL数据库（带pgvector支持）
ups-database:
  image: ankane/pgvector:pg15
  container_name: mini-ups-postgres
  environment:
    POSTGRES_DB: ups_db
    POSTGRES_USER: postgres
    POSTGRES_PASSWORD: abc123
    TZ: UTC
  ports:
    - "5431:5432"
  volumes:
    - postgres_data:/var/lib/postgresql/data
    - ./database/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U postgres"]
    interval: 10s
    timeout: 5s
    retries: 5
```

### 1.3 启动服务

```bash
# 启动PostgreSQL数据库
docker-compose up ups-database -d

# 等待数据库启动
docker logs mini-ups-postgres
```

## 第二部分：数据库设计

### 2.1 RAG数据表结构

#### 核心表：rag_document_chunk

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE rag_document_chunk (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id VARCHAR(255) NOT NULL,
    source VARCHAR(500) NOT NULL,
    chunk_index INTEGER NOT NULL DEFAULT 0,
    content TEXT NOT NULL,
    content_tsv TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED,
    metadata JSONB DEFAULT '{}',
    embedding VECTOR(1536),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 向量相似度索引（HNSW算法）
CREATE INDEX idx_rag_chunk_embedding ON rag_document_chunk
    USING hnsw (embedding vector_cosine_ops);

-- 全文搜索索引
CREATE INDEX idx_rag_chunk_content_tsv ON rag_document_chunk
    USING gin(content_tsv);

-- 查询优化索引
CREATE INDEX idx_rag_chunk_document_id ON rag_document_chunk(document_id);
CREATE INDEX idx_rag_chunk_source ON rag_document_chunk(source);
```

#### 查询日志表：rag_query_log

```sql
CREATE TABLE rag_query_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    role VARCHAR(50),
    query TEXT NOT NULL,
    answer TEXT,
    confidence DOUBLE PRECISION,
    sources TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    feedback VARCHAR(20),
    feedback_comment TEXT,
    feedback_at TIMESTAMP
);

CREATE INDEX idx_rag_query_user_id ON rag_query_log(user_id);
CREATE INDEX idx_rag_query_created_at ON rag_query_log(created_at);
```

### 2.2 初始化脚本

创建 `database/init.sql`:

```sql
-- 启用必要的扩展
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 设置vector相关参数
ALTER SYSTEM SET shared_preload_libraries = 'vector';

-- 这些表会由Spring Boot JPA自动创建，这里只是预设置
```

## 第三部分：后端实现

### 3.1 配置类

#### RagProperties.java - 配置管理

```java
@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private boolean enabled = true;
    private Embedding embedding = new Embedding();
    private Llm llm = new Llm();
    private Retrieval retrieval = new Retrieval();
    private RateLimit rateLimit = new RateLimit();
    private Storage storage = new Storage();
    private Providers providers = new Providers();
    private Ingestion ingestion = new Ingestion();

    @Data
    public static class Embedding {
        private String provider = "openrouter";
        private String model = "text-embedding-3-small";
        private int dimensions = 1536;
    }

    @Data
    public static class Llm {
        private String provider = "openrouter";
        private String model = "openai/gpt-4o-mini";
        private double temperature = 0.2;
        private int maxOutputTokens = 800;
    }

    @Data
    public static class Retrieval {
        private int topK = 5;
        private double similarityThreshold = 0.7;
        private double semanticWeight = 0.7;
        private double keywordWeight = 0.3;
    }

    @Data
    public static class RateLimit {
        private int admin = 100;
        private int dispatcher = 50;
        private int driver = 20;
    }

    @Data
    public static class Storage {
        private String tableName = "rag_document_chunk";
        private int ivfLists = 100;
    }

    @Data
    public static class Providers {
        private OpenRouter openrouter = new OpenRouter();

        @Data
        public static class OpenRouter {
            private String apiKey;
            private String baseUrl = "https://openrouter.ai/api/v1";
            private String siteUrl = "http://localhost";
            private String appName = "Mini-UPS RAG";
        }
    }

    @Data
    public static class Ingestion {
        private boolean enabled = true;
        private List<String> rootPaths = new ArrayList<>(List.of("knowledge"));
        private int chunkSize = 1000;
        private int chunkOverlap = 200;
        private boolean scheduleEnabled = false;
        private String scheduleCron = "0 30 2 * * *";
    }
}
```

#### application.yml配置

```yaml
rag:
  enabled: ${RAG_ENABLED:true}
  embedding:
    provider: ${RAG_EMBEDDING_PROVIDER:openrouter}
    model: ${RAG_EMBEDDING_MODEL:text-embedding-3-small}
    dimensions: ${RAG_EMBEDDING_DIMENSIONS:1536}
  llm:
    provider: ${RAG_LLM_PROVIDER:openrouter}
    model: ${RAG_LLM_MODEL:openai/gpt-4o-mini}
    temperature: ${RAG_LLM_TEMPERATURE:0.2}
    max-output-tokens: ${RAG_LLM_MAX_OUTPUT_TOKENS:800}
  retrieval:
    top-k: ${RAG_RETRIEVAL_TOP_K:5}
    similarity-threshold: ${RAG_RETRIEVAL_SIMILARITY_THRESHOLD:0.7}
    semantic-weight: ${RAG_RETRIEVAL_SEMANTIC_WEIGHT:0.7}
    keyword-weight: ${RAG_RETRIEVAL_KEYWORD_WEIGHT:0.3}
  rate-limit:
    admin: ${RAG_RATE_LIMIT_ADMIN:100}
    dispatcher: ${RAG_RATE_LIMIT_DISPATCHER:50}
    driver: ${RAG_RATE_LIMIT_DRIVER:20}
  storage:
    table-name: ${RAG_STORAGE_TABLE_NAME:rag_document_chunk}
    ivf-lists: ${RAG_STORAGE_IVF_LISTS:100}
  providers:
    openrouter:
      api-key: ${OPENROUTER_API_KEY:}
      base-url: ${OPENROUTER_BASE_URL:https://openrouter.ai/api/v1}
      site-url: ${OPENROUTER_SITE_URL:http://localhost}
      app-name: ${OPENROUTER_APP_NAME:Mini-UPS RAG}
  ingestion:
    enabled: ${RAG_INGESTION_ENABLED:true}
    root-paths: ${RAG_INGESTION_ROOT_PATHS:knowledge}
    chunk-size: ${RAG_INGESTION_CHUNK_SIZE:1000}
    chunk-overlap: ${RAG_INGESTION_CHUNK_OVERLAP:200}
    schedule-enabled: ${RAG_INGESTION_SCHEDULE_ENABLED:false}
    schedule-cron: ${RAG_INGESTION_SCHEDULE_CRON:0 30 2 * * *}
```

### 3.2 核心检索组件

#### RagRetriever.java - 混合检索实现

```java
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagRetriever {

    private static final int MIN_SEARCH_WINDOW = 8;

    private final JdbcTemplate jdbcTemplate;
    private final RagProperties properties;
    private final ObjectMapper objectMapper;

    public List<RagSearchResult> hybridSearch(String query, float[] queryVector, int topK, double similarityThreshold) {
        if (queryVector == null || queryVector.length == 0) {
            return List.of();
        }

        int searchWindow = Math.max(MIN_SEARCH_WINDOW, topK * 2);

        // 并行执行语义搜索和关键词搜索
        List<RagSearchResult> semanticResults = searchByVector(queryVector, searchWindow);
        List<RagSearchResult> keywordResults = searchByKeyword(query, searchWindow);

        // 获取权重配置
        double semanticWeight = Math.max(0.0, properties.getRetrieval().getSemanticWeight());
        double keywordWeight = Math.max(0.0, properties.getRetrieval().getKeywordWeight());
        double weightSum = semanticWeight + keywordWeight;
        if (weightSum <= 0) {
            semanticWeight = 1.0;
            keywordWeight = 0.0;
            weightSum = 1.0;
        }
        double semanticShare = semanticWeight / weightSum;
        double keywordShare = keywordWeight / weightSum;

        // 聚合相同文档块的分数
        Map<UUID, AggregatedResult> aggregated = new LinkedHashMap<>();

        // 处理语义搜索结果
        for (RagSearchResult semantic : semanticResults) {
            AggregatedResult agg = aggregated.computeIfAbsent(semantic.id(), id -> AggregatedResult.from(semantic));
            agg.semanticScore = Math.max(agg.semanticScore, clamp01(semantic.semanticScore()));
        }

        // 处理关键词搜索结果（需要归一化）
        double maxKeywordScore = keywordResults.stream()
            .mapToDouble(RagSearchResult::keywordScore)
            .max()
            .orElse(0.0);

        for (RagSearchResult keyword : keywordResults) {
            double normalized = maxKeywordScore > 0 ? keyword.keywordScore() / maxKeywordScore : 0.0;
            AggregatedResult agg = aggregated.computeIfAbsent(keyword.id(), id -> AggregatedResult.from(keyword));
            agg.keywordScore = Math.max(agg.keywordScore, clamp01(normalized));
        }

        // 计算最终分数并过滤
        List<RagSearchResult> finalResults = new ArrayList<>();
        for (AggregatedResult agg : aggregated.values()) {
            // 相似度阈值过滤
            if (agg.semanticScore > 0 && agg.semanticScore < similarityThreshold) {
                continue;
            }
            // 排除无效结果
            if (agg.semanticScore == 0.0 && agg.keywordScore == 0.0) {
                continue;
            }
            // 加权计算最终分数
            double finalScore = clamp01((semanticShare * agg.semanticScore) + (keywordShare * agg.keywordScore));
            finalResults.add(agg.toResult(finalScore));
        }

        // 按最终分数排序并截取TopK
        finalResults.sort((left, right) -> Double.compare(right.finalScore(), left.finalScore()));
        if (finalResults.size() > topK) {
            return new ArrayList<>(finalResults.subList(0, topK));
        }
        return finalResults;
    }

    // 语义搜索：基于向量相似度
    private List<RagSearchResult> searchByVector(float[] queryVector, int limit) {
        String tableName = properties.resolveTableName();
        String sql = "SELECT id, document_id, source, chunk_index, content, metadata, (embedding <=> ?) AS distance " +
            "FROM " + tableName + " " +
            "ORDER BY embedding <=> ? " +
            "LIMIT ?";
        try {
            return jdbcTemplate.query(sql, ps -> {
                PGvector vector = new PGvector(queryVector);
                ps.setObject(1, vector);
                ps.setObject(2, vector);
                ps.setInt(3, limit);
            }, new SemanticRowMapper());
        } catch (DataAccessException ex) {
            log.error("Vector search failed", ex);
            return List.of();
        }
    }

    // 关键词搜索：基于PostgreSQL全文搜索
    private List<RagSearchResult> searchByKeyword(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        String tableName = properties.resolveTableName();
        String trimmed = query.trim();
        String sql = "SELECT id, document_id, source, chunk_index, content, metadata, " +
            "ts_rank_cd(content_tsv, websearch_to_tsquery('simple', ?)) AS keyword_score " +
            "FROM " + tableName + " " +
            "WHERE content_tsv @@ websearch_to_tsquery('simple', ?) " +
            "ORDER BY keyword_score DESC " +
            "LIMIT ?";
        try {
            return jdbcTemplate.query(sql, ps -> {
                ps.setString(1, trimmed);
                ps.setString(2, trimmed);
                ps.setInt(3, limit);
            }, new KeywordRowMapper());
        } catch (DataAccessException ex) {
            log.error("Keyword search failed", ex);
            return List.of();
        }
    }

    // 分数规范化到[0,1]范围
    private double clamp01(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    // 内部聚合类，用于合并同一文档块的多个搜索结果
    private static final class AggregatedResult {
        private final UUID id;
        private final String documentId;
        private final String source;
        private final int chunkIndex;
        private final String content;
        private final Map<String, Object> metadata;
        private double semanticScore;
        private double keywordScore;

        // 构造方法和其他方法...
    }

    // 语义搜索结果映射器
    private final class SemanticRowMapper implements RowMapper<RagSearchResult> {
        @Override
        public RagSearchResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            UUID id = (UUID) rs.getObject("id");
            String documentId = rs.getString("document_id");
            String source = rs.getString("source");
            int chunkIndex = rs.getInt("chunk_index");
            String content = rs.getString("content");
            double distance = rs.getDouble("distance");
            double semanticScore = clamp01(1 - distance); // 距离转换为相似度
            Map<String, Object> metadata = parseMetadata(rs.getObject("metadata"));

            return new RagSearchResult(
                id, documentId, source, chunkIndex, content,
                semanticScore, 0.0, semanticScore, metadata
            );
        }
    }

    // 关键词搜索结果映射器
    private final class KeywordRowMapper implements RowMapper<RagSearchResult> {
        @Override
        public RagSearchResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            UUID id = (UUID) rs.getObject("id");
            String documentId = rs.getString("document_id");
            String source = rs.getString("source");
            int chunkIndex = rs.getInt("chunk_index");
            String content = rs.getString("content");
            double keywordScore = Math.max(0.0, rs.getDouble("keyword_score"));
            Map<String, Object> metadata = parseMetadata(rs.getObject("metadata"));

            return new RagSearchResult(
                id, documentId, source, chunkIndex, content,
                0.0, keywordScore, 0.0, metadata
            );
        }
    }
}
```

### 3.3 嵌入服务

#### RagEmbeddingClient.java - 向量生成

```java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagEmbeddingClient {

    private final RagProperties properties;
    private final WebClient.Builder webClientBuilder;

    @PostConstruct
    private void init() {
        this.webClient = webClientBuilder
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
            .build();
    }

    private WebClient webClient;

    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        try {
            // 构建请求体
            Map<String, Object> requestBody = Map.of(
                "model", properties.getEmbedding().getModel(),
                "input", texts,
                "encoding_format", "float"
            );

            // 调用OpenRouter API
            EmbeddingResponse response = webClient.post()
                .uri(properties.getProviders().getOpenrouter().getBaseUrl() + "/embeddings")
                .header("Authorization", "Bearer " + properties.getProviders().getOpenrouter().getApiKey())
                .header("HTTP-Referer", properties.getProviders().getOpenrouter().getSiteUrl())
                .header("X-Title", properties.getProviders().getOpenrouter().getAppName())
                .header("Content-Type", "application/json")
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response1 -> {
                    return response1.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("Embedding API error: {}", errorBody);
                            return Mono.error(new RuntimeException("Embedding API failed: " + errorBody));
                        });
                })
                .bodyToMono(EmbeddingResponse.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            if (response == null || response.data() == null) {
                throw new RuntimeException("Empty embedding response");
            }

            // 提取向量数据
            return response.data().stream()
                .sorted(Comparator.comparingInt(EmbeddingData::index))
                .map(data -> {
                    List<Double> embedding = data.embedding();
                    if (embedding == null || embedding.size() != properties.getEmbedding().getDimensions()) {
                        throw new RuntimeException("Invalid embedding dimensions");
                    }
                    return embedding.stream()
                        .map(Double::floatValue)
                        .collect(Collectors.toList())
                        .toArray(new float[0]);
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to generate embeddings", e);
            throw new RuntimeException("Embedding generation failed", e);
        }
    }

    // 响应数据结构
    private record EmbeddingResponse(
        String object,
        List<EmbeddingData> data,
        String model,
        Usage usage
    ) {}

    private record EmbeddingData(
        String object,
        int index,
        List<Double> embedding
    ) {}

    private record Usage(
        int prompt_tokens,
        int total_tokens
    ) {}
}
```

### 3.4 LLM生成服务

#### RagChatClient.java - 答案生成

```java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagChatClient {

    private final RagProperties properties;
    private final WebClient.Builder webClientBuilder;

    @PostConstruct
    private void init() {
        this.webClient = webClientBuilder
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
            .build();
    }

    private WebClient webClient;

    public String generate(String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        try {
            // 构建消息
            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            );

            // 构建请求体
            Map<String, Object> requestBody = Map.of(
                "model", properties.getLlm().getModel(),
                "messages", messages,
                "temperature", temperature,
                "max_tokens", maxTokens,
                "stream", false
            );

            // 调用OpenRouter API
            ChatResponse response = webClient.post()
                .uri(properties.getProviders().getOpenrouter().getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + properties.getProviders().getOpenrouter().getApiKey())
                .header("HTTP-Referer", properties.getProviders().getOpenrouter().getSiteUrl())
                .header("X-Title", properties.getProviders().getOpenrouter().getAppName())
                .header("Content-Type", "application/json")
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response1 -> {
                    return response1.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("Chat API error: {}", errorBody);
                            return Mono.error(new RuntimeException("Chat API failed: " + errorBody));
                        });
                })
                .bodyToMono(ChatResponse.class)
                .timeout(Duration.ofSeconds(60))
                .block();

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new RuntimeException("Empty chat response");
            }

            return response.choices().get(0).message().content();

        } catch (Exception e) {
            log.error("Failed to generate chat response", e);
            throw new RuntimeException("Chat generation failed", e);
        }
    }

    // 响应数据结构
    private record ChatResponse(
        String id,
        String object,
        long created,
        String model,
        List<Choice> choices,
        Usage usage
    ) {}

    private record Choice(
        int index,
        Message message,
        String finish_reason
    ) {}

    private record Message(
        String role,
        String content
    ) {}

    private record Usage(
        int prompt_tokens,
        int completion_tokens,
        int total_tokens
    ) {}
}
```

### 3.5 查询服务

#### RagQueryService.java - 主要业务逻辑

```java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagQueryService {

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
            // 1. 速率限制检查
            enforceRateLimit(userContext);

            String query = request.getQuery();
            if (!StringUtils.hasText(query)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query must not be blank");
            }

            // 2. 生成查询向量
            List<float[]> vectors = embeddingClient.embed(List.of(query));
            if (CollectionUtils.isEmpty(vectors)) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate query embedding");
            }
            float[] queryVector = vectors.get(0);

            // 3. 混合检索
            int topK = Math.max(1, properties.getRetrieval().getTopK());
            double threshold = properties.getRetrieval().getSimilarityThreshold();
            List<RagSearchResult> searchResults = retriever.hybridSearch(query, queryVector, topK, threshold);

            // 4. 处理空结果
            if (searchResults.isEmpty()) {
                recordEmptyResultMetrics(userContext);
                return buildEmptyResponse();
            }

            // 5. 构建Prompt并生成答案
            String prompt = buildUserPrompt(query, searchResults, userContext);
            String answer = chatClient.generate(
                SYSTEM_PROMPT,
                prompt,
                properties.getLlm().getTemperature(),
                properties.getLlm().getMaxOutputTokens()
            );

            // 6. 计算置信度和构建响应
            double confidence = computeConfidence(searchResults);
            List<RagSourceDto> sources = mapSources(searchResults);

            // 7. 记录指标和日志
            recordRetrievalMetrics(userContext, searchResults);
            UUID logId = persistQueryLog(userContext, query, answer, confidence, sources);

            // 8. 构建最终响应
            RagQueryResponse response = RagQueryResponse.builder()
                .logId(logId)
                .answer(answer)
                .confidence(confidence)
                .sources(sources)
                .build();

            recordSuccessMetrics(userContext, sample);
            return response;

        } catch (RuntimeException ex) {
            recordErrorMetrics(userContext, sample);
            throw ex;
        }
    }

    // 构建用户Prompt
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

    // 计算置信度
    private double computeConfidence(List<RagSearchResult> results) {
        double max = 0.0;
        for (RagSearchResult result : results) {
            max = Math.max(max, result.finalScore());
        }
        return Math.min(1.0, Math.max(0.0, max));
    }

    // 映射源数据
    private List<RagSourceDto> mapSources(List<RagSearchResult> results) {
        List<RagSourceDto> sources = new ArrayList<>();
        for (RagSearchResult result : results) {
            String title = Objects.toString(result.metadata().getOrDefault("title", result.source()), result.source());
            sources.add(RagSourceDto.builder()
                .title(title)
                .source(result.source())
                .similarity(result.finalScore())
                .confidence(result.finalScore())
                .semanticScore(result.semanticScore())
                .keywordScore(result.keywordScore())
                .build());
        }
        return sources;
    }

    // 其他辅助方法...
}
```

### 3.6 控制器

#### RagController.java - REST API

```java
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagController {

    private final RagQueryService queryService;

    @PostMapping("/query")
    public ResponseEntity<RagQueryResponse> query(
        @Valid @RequestBody RagQueryRequest request,
        Authentication authentication
    ) {
        try {
            RagQueryResponse response = queryService.handleQuery(request, authentication);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("RAG query failed", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RagQueryResponse.builder()
                    .answer("系统暂时不可用，请稍后重试。")
                    .confidence(0.0)
                    .sources(List.of())
                    .warnings(List.of("系统内部错误"))
                    .build());
        }
    }

    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(
        @Valid @RequestBody RagFeedbackRequest request,
        Authentication authentication
    ) {
        // 反馈处理逻辑...
        return ResponseEntity.ok().build();
    }
}
```

## 第四部分：前端实现

### 4.1 RAG服务层

#### rag.ts - API调用

```typescript
import api from './api'

export interface RagQueryPayload {
  query: string
  context?: Record<string, unknown>
}

export interface RagSourceResponse {
  title: string
  source: string
  similarity: number
  confidence: number
  semanticScore?: number
  keywordScore?: number
}

export interface RagQueryResult {
  logId?: string
  answer: string
  confidence: number
  sources: RagSourceResponse[]
  warnings?: string[]
}

export interface RagFeedbackPayload {
  logId: string
  feedback: 'POSITIVE' | 'NEGATIVE'
  comment?: string
}

export async function queryRag(payload: RagQueryPayload): Promise<RagQueryResult> {
  const { data } = await api.post<RagQueryResult>('/rag/query', payload)
  return data
}

export async function submitRagFeedback(payload: RagFeedbackPayload): Promise<void> {
  await api.post('/rag/feedback', payload)
}
```

### 4.2 RAG组件

#### RagAssistant.tsx - React组件

```tsx
import { useCallback, useState } from 'react'
import { Loader2, MessageCircle, Send, ThumbsUp, ThumbsDown } from 'lucide-react'
import { useAuthStore } from '@/stores/auth-store'
import { queryRag, submitRagFeedback, type RagQueryResult, type RagSourceResponse } from '@/services/rag'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Badge } from '@/components/ui/badge'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import { toast } from 'sonner'

interface RagAssistantMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  createdAt: Date
  sources?: RagSourceResponse[]
  warnings?: string[]
  logId?: string
  feedback?: 'positive' | 'negative'
  feedbackSubmitting?: boolean
}

const SUGGESTED_QUERIES = [
  '如何处理延迟配送？',
  '司机签到流程是什么？',
  '如何同步世界模拟器状态？',
]

export const RagAssistant = () => {
  const { user } = useAuthStore()
  const [open, setOpen] = useState(false)
  const [input, setInput] = useState('')
  const [messages, setMessages] = useState<RagAssistantMessage[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = useCallback(async () => {
    const query = input.trim()
    if (!query) return

    // 添加用户消息
    const userMessage: RagAssistantMessage = {
      id: createId(),
      role: 'user',
      content: query,
      createdAt: new Date(),
    }

    setMessages((prev) => [...prev, userMessage])
    setInput('')
    setError(null)
    setLoading(true)

    try {
      const payload = {
        query,
        context: {
          role: user?.role,
        },
      }

      const result: RagQueryResult = await queryRag(payload)

      const assistantMessage: RagAssistantMessage = {
        id: createId(),
        role: 'assistant',
        content: result.answer,
        createdAt: new Date(),
        sources: result.sources,
        warnings: result.warnings,
        logId: result.logId,
      }

      setMessages((prev) => [...prev, assistantMessage])
    } catch (err) {
      console.error('RAG query failed', err)
      setError('助手暂时不可用，请稍后再试。')

      const fallbackMessage: RagAssistantMessage = {
        id: createId(),
        role: 'assistant',
        content: '抱歉，当前无法获取答案，请稍后重试。',
        createdAt: new Date(),
      }

      setMessages((prev) => [...prev, fallbackMessage])
    } finally {
      setLoading(false)
    }
  }, [input, user?.role])

  const handleFeedback = useCallback(async (messageId: string, feedback: 'positive' | 'negative') => {
    const message = messages.find(m => m.id === messageId)
    if (!message?.logId) return

    setMessages(prev => prev.map(m =>
      m.id === messageId
        ? { ...m, feedbackSubmitting: true }
        : m
    ))

    try {
      await submitRagFeedback({
        logId: message.logId,
        feedback: feedback.toUpperCase() as 'POSITIVE' | 'NEGATIVE'
      })

      setMessages(prev => prev.map(m =>
        m.id === messageId
          ? { ...m, feedback, feedbackSubmitting: false }
          : m
      ))

      toast.success('反馈已提交，谢谢！')
    } catch (err) {
      console.error('Failed to submit feedback', err)
      toast.error('反馈提交失败')

      setMessages(prev => prev.map(m =>
        m.id === messageId
          ? { ...m, feedbackSubmitting: false }
          : m
      ))
    }
  }, [messages])

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button variant="outline" size="icon" className="fixed bottom-4 right-4 h-12 w-12 rounded-full shadow-lg">
          <MessageCircle className="h-6 w-6" />
        </Button>
      </SheetTrigger>
      <SheetContent className="w-[400px] sm:w-[540px]">
        <SheetHeader>
          <SheetTitle className="flex items-center gap-2">
            <MessageCircle className="h-5 w-5" />
            智能助手
          </SheetTitle>
        </SheetHeader>

        <div className="flex h-full max-h-[calc(100vh-120px)] flex-col">
          {/* 消息区域 */}
          <ScrollArea className="flex-1 pr-4">
            <div className="space-y-4 py-4">
              {messages.length === 0 && (
                <div className="space-y-3">
                  <p className="text-sm text-muted-foreground">
                    你好！我是 Mini-UPS 智能助手，可以帮您解答操作问题。
                  </p>
                  <div className="space-y-2">
                    <p className="text-xs font-medium text-muted-foreground">建议问题：</p>
                    {SUGGESTED_QUERIES.map((query, index) => (
                      <button
                        key={index}
                        onClick={() => setInput(query)}
                        className="block w-full rounded-lg border border-dashed border-muted-foreground/25 p-2 text-left text-xs hover:bg-muted/50"
                      >
                        {query}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {messages.map((message) => (
                <div key={message.id} className={`flex gap-3 ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                  <div className={`max-w-[80%] space-y-2 ${message.role === 'user' ? 'order-2' : ''}`}>
                    <div className={`rounded-lg px-3 py-2 text-sm ${
                      message.role === 'user'
                        ? 'bg-primary text-primary-foreground'
                        : 'bg-muted'
                    }`}>
                      {message.content}
                    </div>

                    {/* 源引用 */}
                    {message.sources && message.sources.length > 0 && (
                      <div className="space-y-1">
                        <p className="text-xs font-medium text-muted-foreground">参考来源：</p>
                        {message.sources.map((source, index) => (
                          <div key={index} className="flex items-center gap-2 rounded border p-2 text-xs">
                            <Badge variant="outline" className="shrink-0">
                              {index + 1}
                            </Badge>
                            <div className="min-w-0 flex-1">
                              <p className="truncate font-medium">{source.title}</p>
                              <p className="text-muted-foreground">
                                相关度: {(source.similarity * 100).toFixed(1)}%
                              </p>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* 反馈按钮 */}
                    {message.role === 'assistant' && message.logId && (
                      <div className="flex gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleFeedback(message.id, 'positive')}
                          disabled={message.feedbackSubmitting || message.feedback === 'positive'}
                          className="h-7 px-2"
                        >
                          <ThumbsUp className="h-3 w-3" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleFeedback(message.id, 'negative')}
                          disabled={message.feedbackSubmitting || message.feedback === 'negative'}
                          className="h-7 px-2"
                        >
                          <ThumbsDown className="h-3 w-3" />
                        </Button>
                      </div>
                    )}

                    {/* 警告信息 */}
                    {message.warnings && message.warnings.length > 0 && (
                      <div className="rounded border border-warning bg-warning/10 p-2">
                        {message.warnings.map((warning, index) => (
                          <p key={index} className="text-xs text-warning-foreground">{warning}</p>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              ))}

              {loading && (
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  正在思考...
                </div>
              )}

              {error && (
                <div className="rounded border border-destructive bg-destructive/10 p-2 text-sm text-destructive">
                  {error}
                </div>
              )}
            </div>
          </ScrollArea>

          {/* 输入区域 */}
          <div className="border-t pt-4">
            <div className="flex gap-2">
              <Textarea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="请输入您的问题..."
                className="min-h-[60px] resize-none"
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault()
                    handleSubmit()
                  }
                }}
              />
              <Button
                onClick={handleSubmit}
                disabled={!input.trim() || loading}
                size="icon"
                className="shrink-0"
              >
                <Send className="h-4 w-4" />
              </Button>
            </div>
            <p className="mt-2 text-xs text-muted-foreground">
              按 Enter 发送，Shift+Enter 换行
            </p>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
}

const createId = () => {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return Math.random().toString(36).slice(2)
}
```

## 第五部分：数据摄取

### 5.1 文档摄取服务

#### RagIngestionService.java - 批量处理文档

```java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagIngestionService {

    private final RagProperties properties;
    private final RagEmbeddingClient embeddingClient;
    private final RagChunkWriter chunkWriter;
    private final RagTextChunker textChunker;
    private final FileSystemDocumentLoader documentLoader;

    public RagIngestionJobSummary ingestDocuments() {
        if (!properties.getIngestion().isEnabled()) {
            return RagIngestionJobSummary.builder()
                .status("DISABLED")
                .message("RAG ingestion is disabled")
                .build();
        }

        try {
            long startTime = System.currentTimeMillis();

            // 1. 加载文档
            List<RagDocumentResource> documents = loadDocuments();

            // 2. 分块处理
            List<RagDocumentChunk> chunks = new ArrayList<>();
            for (RagDocumentResource doc : documents) {
                List<RagDocumentChunk> docChunks = textChunker.chunk(
                    doc.getSource(),
                    doc.getContent(),
                    doc.getMetadata(),
                    properties.getIngestion().getChunkSize(),
                    properties.getIngestion().getChunkOverlap()
                );
                chunks.addAll(docChunks);
            }

            // 3. 批量生成嵌入向量
            List<String> texts = chunks.stream()
                .map(RagDocumentChunk::getContent)
                .collect(Collectors.toList());

            List<float[]> embeddings = embeddingClient.embed(texts);

            // 4. 设置向量并保存
            for (int i = 0; i < chunks.size(); i++) {
                chunks.get(i).setEmbedding(embeddings.get(i));
            }

            chunkWriter.saveChunks(chunks);

            long duration = System.currentTimeMillis() - startTime;

            return RagIngestionJobSummary.builder()
                .status("SUCCESS")
                .documentsProcessed(documents.size())
                .chunksCreated(chunks.size())
                .durationMs(duration)
                .message(String.format("Successfully processed %d documents into %d chunks",
                    documents.size(), chunks.size()))
                .build();

        } catch (Exception e) {
            log.error("Document ingestion failed", e);
            return RagIngestionJobSummary.builder()
                .status("ERROR")
                .message("Ingestion failed: " + e.getMessage())
                .build();
        }
    }

    private List<RagDocumentResource> loadDocuments() {
        List<RagDocumentResource> documents = new ArrayList<>();

        for (String rootPath : properties.getIngestion().getRootPaths()) {
            try {
                List<RagDocumentResource> pathDocs = documentLoader.loadFromPath(rootPath);
                documents.addAll(pathDocs);
            } catch (Exception e) {
                log.warn("Failed to load documents from path: {}", rootPath, e);
            }
        }

        return documents;
    }
}
```

### 5.2 文本分块器

#### RagTextChunker.java - 文本分割

```java
@Component
@RequiredArgsConstructor
public class RagTextChunker {

    public List<RagDocumentChunk> chunk(String source, String content, Map<String, Object> metadata, int chunkSize, int overlap) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }

        List<RagDocumentChunk> chunks = new ArrayList<>();
        String documentId = UUID.randomUUID().toString();

        // 简单分块策略：按句子分割，保持重叠
        List<String> sentences = splitIntoSentences(content);
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);

            // 检查是否超过块大小
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                // 保存当前块
                RagDocumentChunk chunk = new RagDocumentChunk();
                chunk.setDocumentId(documentId);
                chunk.setSource(source);
                chunk.setChunkIndex(chunkIndex++);
                chunk.setContent(currentChunk.toString().trim());
                chunk.setMetadata(metadata != null ? metadata : Map.of());
                chunks.add(chunk);

                // 开始新块，包含重叠内容
                currentChunk = new StringBuilder();
                if (overlap > 0 && chunks.size() > 0) {
                    String previousContent = chunks.get(chunks.size() - 1).getContent();
                    String overlapText = extractOverlap(previousContent, overlap);
                    currentChunk.append(overlapText).append(" ");
                }
            }

            currentChunk.append(sentence).append(" ");
        }

        // 保存最后一块
        if (currentChunk.length() > 0) {
            RagDocumentChunk chunk = new RagDocumentChunk();
            chunk.setDocumentId(documentId);
            chunk.setSource(source);
            chunk.setChunkIndex(chunkIndex);
            chunk.setContent(currentChunk.toString().trim());
            chunk.setMetadata(metadata != null ? metadata : Map.of());
            chunks.add(chunk);
        }

        return chunks;
    }

    private List<String> splitIntoSentences(String text) {
        // 简单句子分割，可以根据需要改进
        return Arrays.stream(text.split("[.!?。！？]"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    private String extractOverlap(String text, int overlapChars) {
        if (text.length() <= overlapChars) {
            return text;
        }
        return text.substring(text.length() - overlapChars);
    }
}
```

### 5.3 文档加载器

#### FileSystemDocumentLoader.java - 文件系统加载

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class FileSystemDocumentLoader {

    public List<RagDocumentResource> loadFromPath(String rootPath) throws IOException {
        List<RagDocumentResource> documents = new ArrayList<>();
        Path root = Paths.get(rootPath);

        if (!Files.exists(root)) {
            log.warn("Root path does not exist: {}", rootPath);
            return documents;
        }

        Files.walk(root)
            .filter(Files::isRegularFile)
            .filter(this::isSupportedFile)
            .forEach(path -> {
                try {
                    RagDocumentResource doc = loadFile(path);
                    if (doc != null) {
                        documents.add(doc);
                    }
                } catch (Exception e) {
                    log.warn("Failed to load file: {}", path, e);
                }
            });

        return documents;
    }

    private boolean isSupportedFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".txt") ||
               fileName.endsWith(".md") ||
               fileName.endsWith(".rst") ||
               fileName.endsWith(".json");
    }

    private RagDocumentResource loadFile(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        if (!StringUtils.hasText(content)) {
            return null;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filename", path.getFileName().toString());
        metadata.put("path", path.toString());
        metadata.put("size", Files.size(path));
        metadata.put("lastModified", Files.getLastModifiedTime(path).toInstant().toString());

        // 从文件名推断标题
        String title = path.getFileName().toString();
        int lastDot = title.lastIndexOf('.');
        if (lastDot > 0) {
            title = title.substring(0, lastDot);
        }
        metadata.put("title", title);

        return RagDocumentResource.builder()
            .source(path.toString())
            .content(content)
            .metadata(metadata)
            .build();
    }
}
```

## 第六部分：测试和部署

### 6.1 环境变量设置

创建 `.env` 文件：

```bash
# RAG配置
RAG_ENABLED=true

# OpenRouter API配置
OPENROUTER_API_KEY=your_openrouter_api_key_here
OPENROUTER_BASE_URL=https://openrouter.ai/api/v1
OPENROUTER_SITE_URL=http://localhost:3000
OPENROUTER_APP_NAME=Mini-UPS RAG

# 嵌入模型配置
RAG_EMBEDDING_PROVIDER=openrouter
RAG_EMBEDDING_MODEL=text-embedding-3-small
RAG_EMBEDDING_DIMENSIONS=1536

# LLM配置
RAG_LLM_PROVIDER=openrouter
RAG_LLM_MODEL=openai/gpt-4o-mini
RAG_LLM_TEMPERATURE=0.2
RAG_LLM_MAX_OUTPUT_TOKENS=800

# 检索配置
RAG_RETRIEVAL_TOP_K=5
RAG_RETRIEVAL_SIMILARITY_THRESHOLD=0.7
RAG_RETRIEVAL_SEMANTIC_WEIGHT=0.7
RAG_RETRIEVAL_KEYWORD_WEIGHT=0.3

# 速率限制
RAG_RATE_LIMIT_ADMIN=100
RAG_RATE_LIMIT_DISPATCHER=50
RAG_RATE_LIMIT_DRIVER=20

# 数据摄取配置
RAG_INGESTION_ENABLED=true
RAG_INGESTION_ROOT_PATHS=knowledge
RAG_INGESTION_CHUNK_SIZE=1000
RAG_INGESTION_CHUNK_OVERLAP=200

# 数据库配置
DATABASE_URL=jdbc:postgresql://localhost:5431/ups_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=abc123
```

### 6.2 启动步骤

#### 1. 启动数据库

```bash
# 启动PostgreSQL数据库（带pgvector）
docker-compose up ups-database -d

# 检查数据库状态
docker logs mini-ups-postgres
```

#### 2. 准备知识库

```bash
# 创建知识库目录
mkdir -p knowledge

# 添加示例文档
cat > knowledge/delivery-guide.md << 'EOF'
# 配送指南

## 延迟配送处理流程

当包裹配送延迟时，请按以下步骤处理：

1. 检查包裹状态和位置
2. 联系客户说明情况
3. 重新安排配送时间
4. 更新系统状态
5. 记录处理结果

## 货物损坏处理

如发现货物损坏：

1. 立即拍照记录
2. 联系客户确认情况
3. 填写损坏报告
4. 启动理赔流程
5. 协调补救措施
EOF

cat > knowledge/tracking-system.md << 'EOF'
# 跟踪号查询系统

## 系统使用方法

用户可以通过以下方式查询包裹状态：

1. 在官网输入跟踪号
2. 使用移动应用扫描二维码
3. 致电客服热线查询
4. 关注微信公众号查询

## 状态说明

- PENDING: 订单已创建，等待揽收
- PICKED_UP: 已揽收，准备运输
- IN_TRANSIT: 运输中
- DELIVERED: 已送达
- EXCEPTION: 异常情况
EOF
```

#### 3. 启动后端服务

```bash
# 编译项目
JAVA_HOME=/opt/homebrew/opt/openjdk@17 mvn clean compile

# 启动Spring Boot应用
JAVA_HOME=/opt/homebrew/opt/openjdk@17 mvn spring-boot:run -Dspring-boot.run.profiles=local
```

#### 4. 启动前端

```bash
# 安装依赖
cd frontend
npm install

# 启动开发服务器
npm run dev
```

### 6.3 数据摄取

#### 手动触发摄取

```bash
# 通过REST API触发文档摄取
curl -X POST http://localhost:8081/api/rag/ingest \
  -H "Authorization: Bearer your_jwt_token" \
  -H "Content-Type: application/json"
```

#### 验证数据摄取

```sql
-- 连接数据库检查数据
psql -h localhost -p 5431 -U postgres -d ups_db

-- 查看摄取的文档块
SELECT id, source, chunk_index,
       LEFT(content, 100) as content_preview,
       metadata->>'title' as title
FROM rag_document_chunk
ORDER BY source, chunk_index;

-- 检查向量数据
SELECT COUNT(*) as total_chunks,
       COUNT(embedding) as chunks_with_embeddings
FROM rag_document_chunk;
```

### 6.4 测试RAG功能

#### 1. 后端API测试

```bash
# 测试查询API
curl -X POST http://localhost:8081/api/rag/query \
  -H "Authorization: Bearer your_jwt_token" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "如何处理延迟配送？",
    "context": {
      "role": "DISPATCHER"
    }
  }'
```

#### 2. 前端界面测试

1. 访问 `http://localhost:3000`
2. 登录系统
3. 点击右下角的智能助手按钮
4. 输入测试问题：
   - "如何处理延迟配送？"
   - "货物损坏了怎么办？"
   - "跟踪号怎么查询？"

#### 3. 功能验证

验证以下功能正常工作：

- ✅ 混合检索（语义+关键词）
- ✅ 相关文档片段检索
- ✅ LLM答案生成
- ✅ 来源引用显示
- ✅ 置信度计算
- ✅ 用户反馈收集
- ✅ 查询日志记录

## 第七部分：高级优化

### 7.1 性能优化

#### 1. 数据库索引优化

```sql
-- 创建复合索引提升查询性能
CREATE INDEX idx_rag_chunk_composite ON rag_document_chunk(document_id, chunk_index);

-- 创建部分索引（只为有向量的记录建索引）
CREATE INDEX idx_rag_chunk_embedding_non_null ON rag_document_chunk
    USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

-- 查询统计信息
ANALYZE rag_document_chunk;
```

#### 2. 连接池优化

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      leak-detection-threshold: 60000
```

#### 3. 缓存配置

```java
@EnableCaching
@Configuration
public class RagCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats());
        return cacheManager;
    }
}
```

### 7.2 监控和观察

#### 1. 指标收集

```java
@Component
public class RagMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter queryCounter;
    private final Timer queryTimer;
    private final DistributionSummary confidenceSummary;

    public RagMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.queryCounter = Counter.builder("rag.queries.total")
            .description("Total number of RAG queries")
            .register(meterRegistry);
        this.queryTimer = Timer.builder("rag.query.duration")
            .description("RAG query execution time")
            .register(meterRegistry);
        this.confidenceSummary = DistributionSummary.builder("rag.confidence")
            .description("RAG response confidence distribution")
            .register(meterRegistry);
    }
}
```

#### 2. 健康检查

```java
@Component
public class RagHealthIndicator implements HealthIndicator {

    private final RagProperties properties;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Health health() {
        try {
            if (!properties.isEnabled()) {
                return Health.up().withDetail("rag", "disabled").build();
            }

            // 检查数据库连接
            Long chunkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + properties.resolveTableName(),
                Long.class
            );

            return Health.up()
                .withDetail("database", "connected")
                .withDetail("chunks", chunkCount)
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### 7.3 安全增强

#### 1. 输入验证和清理

```java
@Component
public class RagSecurityService {

    private static final int MAX_QUERY_LENGTH = 1000;
    private static final Pattern SUSPICIOUS_PATTERN = Pattern.compile(
        "(?i)(script|javascript|eval|exec|system|cmd)"
    );

    public void validateQuery(String query) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        if (query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Query too long");
        }

        if (SUSPICIOUS_PATTERN.matcher(query).find()) {
            throw new IllegalArgumentException("Suspicious query content");
        }
    }
}
```

#### 2. 角色权限控制

```java
@PreAuthorize("hasRole('USER')")
@PostMapping("/query")
public ResponseEntity<RagQueryResponse> query(/* ... */) {
    // 实现逻辑
}

@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/ingest")
public ResponseEntity<RagIngestionJobSummary> ingest(/* ... */) {
    // 实现逻辑
}
```

## 第八部分：故障排查

### 8.1 常见问题

#### 1. pgvector扩展未安装

**症状**: `ERROR: type "vector" does not exist`

**解决方案**:
```sql
-- 连接数据库
psql -h localhost -p 5431 -U postgres -d ups_db

-- 安装扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 验证安装
SELECT * FROM pg_extension WHERE extname = 'vector';
```

#### 2. 向量维度不匹配

**症状**: `ERROR: vector dimension mismatch`

**解决方案**:
```java
// 检查配置的向量维度
@Value("${rag.embedding.dimensions:1536}")
private int embeddingDimensions;

// 验证向量长度
if (embedding.length != embeddingDimensions) {
    throw new IllegalArgumentException("Embedding dimension mismatch");
}
```

#### 3. API密钥错误

**症状**: `401 Unauthorized` 或 `403 Forbidden`

**解决方案**:
```bash
# 检查环境变量
echo $OPENROUTER_API_KEY

# 验证密钥
curl -H "Authorization: Bearer $OPENROUTER_API_KEY" \
     https://openrouter.ai/api/v1/models
```

#### 4. 数据摄取失败

**症状**: 文档块数量为0

**解决方案**:
```java
// 检查文件路径
Path knowledgePath = Paths.get("knowledge");
if (!Files.exists(knowledgePath)) {
    log.error("Knowledge directory does not exist: {}", knowledgePath);
}

// 检查文件权限
try {
    Files.walk(knowledgePath)
        .filter(Files::isRegularFile)
        .forEach(path -> {
            if (!Files.isReadable(path)) {
                log.warn("Cannot read file: {}", path);
            }
        });
} catch (IOException e) {
    log.error("Error reading knowledge directory", e);
}
```

### 8.2 性能问题诊断

#### 1. 慢查询分析

```sql
-- 启用查询统计
SET track_io_timing = on;
SET log_min_duration_statement = 1000;

-- 分析向量查询性能
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, content, (embedding <=> '[1,2,3...]') AS distance
FROM rag_document_chunk
ORDER BY embedding <=> '[1,2,3...]'
LIMIT 10;
```

#### 2. 内存使用监控

```java
@Component
public class RagMemoryMonitor {

    @Scheduled(fixedRate = 30000)
    public void logMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        log.info("Heap memory usage: {} MB / {} MB",
            heapUsage.getUsed() / 1024 / 1024,
            heapUsage.getMax() / 1024 / 1024);
    }
}
```

### 8.3 日志配置

#### application.yml日志配置

```yaml
logging:
  level:
    com.miniups.rag: DEBUG
    org.springframework.jdbc: DEBUG
    com.pgvector: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/rag.log
    max-size: 100MB
    max-history: 30
```

## 总结

通过本指南，你应该能够：

1. ✅ 理解RAG系统的完整架构
2. ✅ 搭建PostgreSQL + pgvector环境
3. ✅ 实现混合检索（语义+关键词）
4. ✅ 集成LLM生成智能答案
5. ✅ 构建用户友好的前端界面
6. ✅ 实现文档摄取和向量化
7. ✅ 配置监控和健康检查
8. ✅ 处理常见问题和性能优化

这个RAG系统具有以下特点：

- **高性能**: 使用pgvector HNSW索引实现毫秒级向量搜索
- **智能检索**: 结合语义搜索和关键词搜索的混合算法
- **用户友好**: 支持多角色权限和实时反馈
- **可扩展**: 模块化设计，易于添加新功能
- **生产就绪**: 包含监控、日志、健康检查等

现在你可以基于这个框架构建自己的RAG应用了！