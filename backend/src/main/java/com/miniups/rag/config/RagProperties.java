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

        // Manual getters
        public String getProvider() { return provider; }
        public String getModel() { return model; }
        public int getDimensions() { return dimensions; }
    }

    @Data
    public static class Llm {
        private String provider = "openrouter";
        private String model = "openai/gpt-4o-mini";
        private double temperature = 0.2;
        private int maxOutputTokens = 800;

        // Manual getters
        public String getProvider() { return provider; }
        public String getModel() { return model; }
        public double getTemperature() { return temperature; }
        public int getMaxOutputTokens() { return maxOutputTokens; }
    }

    @Data
    public static class Retrieval {
        private int topK = 5;
        private double similarityThreshold = 0.7;
        private double semanticWeight = 0.7;
        private double keywordWeight = 0.3;

        // Manual getters
        public int getTopK() { return topK; }
        public double getSimilarityThreshold() { return similarityThreshold; }
        public double getSemanticWeight() { return semanticWeight; }
        public double getKeywordWeight() { return keywordWeight; }
    }

    @Data
    public static class RateLimit {
        private int admin = 100;
        private int dispatcher = 50;
        private int driver = 20;

        // Manual getters
        public int getAdmin() { return admin; }
        public int getDispatcher() { return dispatcher; }
        public int getDriver() { return driver; }
    }

    @Data
    public static class Storage {
        private String tableName = "rag_document_chunk";
        private int ivfLists = 100;

        // Manual getters
        public String getTableName() { return tableName; }
        public int getIvfLists() { return ivfLists; }
    }

    @Data
    public static class Providers {
        private OpenAi openai = new OpenAi();
        private OpenRouter openrouter = new OpenRouter();

        // Manual getters
        public OpenAi getOpenai() { return openai; }
        public OpenRouter getOpenrouter() { return openrouter; }

        @Data
        public static class OpenAi {
            private String apiKey;
            private String baseUrl = "https://api.openai.com/v1";

            // Manual getters
            public String getApiKey() { return apiKey; }
            public String getBaseUrl() { return baseUrl; }
        }

        @Data
        public static class OpenRouter {
            private String apiKey;
            private String baseUrl = "https://openrouter.ai/api/v1";
            private String siteUrl = "http://localhost";
            private String appName = "Mini-UPS RAG";

            // Manual getters
            public String getApiKey() { return apiKey; }
            public String getBaseUrl() { return baseUrl; }
            public String getSiteUrl() { return siteUrl; }
            public String getAppName() { return appName; }
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

        // Manual getters
        public boolean isEnabled() { return enabled; }
        public List<String> getRootPaths() { return rootPaths; }
        public int getChunkSize() { return chunkSize; }
        public int getChunkOverlap() { return chunkOverlap; }
        public boolean isScheduleEnabled() { return scheduleEnabled; }
        public String getScheduleCron() { return scheduleCron; }
    }

    public String resolveTableName() {
        return Objects.requireNonNullElse(storage.tableName, "rag_document_chunk");
    }

    // Manual getters (Lombok @Data not generating them properly)
    public boolean isEnabled() { return enabled; }
    public Embedding getEmbedding() { return embedding; }
    public Llm getLlm() { return llm; }
    public Retrieval getRetrieval() { return retrieval; }
    public RateLimit getRateLimit() { return rateLimit; }
    public Storage getStorage() { return storage; }
    public Providers getProviders() { return providers; }
    public Ingestion getIngestion() { return ingestion; }
}
