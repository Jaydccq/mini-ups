package com.miniups.rag.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
        private OpenAi openai = new OpenAi();
        private OpenRouter openrouter = new OpenRouter();

        @Data
        public static class OpenAi {
            private String apiKey;
            private String baseUrl = "https://api.openai.com/v1";
        }

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

    public String resolveTableName() {
        return Objects.requireNonNullElse(storage.tableName, "rag_document_chunk");
    }
}
