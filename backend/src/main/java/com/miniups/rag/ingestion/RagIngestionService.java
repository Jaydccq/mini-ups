package com.miniups.rag.ingestion;

import com.miniups.rag.config.RagProperties;
import com.miniups.rag.embedding.RagEmbeddingClient;
import com.miniups.rag.ingestion.RagTextChunker.TextChunk;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagIngestionService {


    private static final Logger log = LoggerFactory.getLogger(RagIngestionService.class);
    private static final int DEFAULT_BATCH_SIZE = 16;

    private final RagProperties properties;
    private final FileSystemDocumentLoader documentLoader;
    private final RagTextChunker chunker;
    private final RagEmbeddingClient embeddingClient;
    private final RagChunkWriter chunkWriter;
    private final RagIngestionJobRepository jobRepository;

    // Manual constructor (Lombok @RequiredArgsConstructor not working)
    public RagIngestionService(RagProperties properties,
                              FileSystemDocumentLoader documentLoader,
                              RagTextChunker chunker,
                              RagEmbeddingClient embeddingClient,
                              RagChunkWriter chunkWriter,
                              RagIngestionJobRepository jobRepository) {
        this.properties = properties;
        this.documentLoader = documentLoader;
        this.chunker = chunker;
        this.embeddingClient = embeddingClient;
        this.chunkWriter = chunkWriter;
        this.jobRepository = jobRepository;
    }

    private final AtomicBoolean running = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void ingestOnStartup() {
        if (!properties.isEnabled() || !properties.getIngestion().isEnabled()) {
            log.info("RAG ingestion disabled; skipping startup load");
            return;
        }
        triggerIngestion("STARTUP", false);
    }

    @Scheduled(cron = "${rag.ingestion.schedule-cron:0 30 2 * * *}")
    public void scheduledIngestion() {
        if (!properties.getIngestion().isScheduleEnabled()) {
            return;
        }
        triggerIngestion("SCHEDULE", true);
    }

    public RagIngestionJobSummary triggerManualIngestion() {
        if (!properties.isEnabled() || !properties.getIngestion().isEnabled()) {
            throw new IllegalStateException("RAG ingestion is disabled by configuration");
        }
        log.info("Manual trigger requested for RAG ingestion");
        return triggerIngestion("MANUAL", true);
    }

    public RagIngestionJobSummary latestJob() {
        return jobRepository.findLatest();
    }

    private RagIngestionJobSummary triggerIngestion(String trigger, boolean failIfRunning) {
        if (failIfRunning) {
            if (!running.compareAndSet(false, true)) {
                throw new IllegalStateException("RAG ingestion is already running");
            }
        } else {
            if (!running.compareAndSet(false, true)) {
                log.info("RAG ingestion already in progress; skip {} trigger", trigger);
                return null;
            }
        }

        UUID jobId = null;
        int stored = 0;
        int documentsCount = 0;
        try {
            log.info("Starting RAG ingestion job (trigger={})", trigger);
            jobId = jobRepository.startJob(trigger);
            List<RagDocumentResource> documents = documentLoader.loadDocuments();
            documentsCount = documents.size();
            if (documents.isEmpty()) {
                jobRepository.markCompleted(jobId, 0, 0);
                return jobRepository.findById(jobId);
            }

            List<ChunkContext> contexts = new ArrayList<>();
            for (RagDocumentResource resource : documents) {
                List<TextChunk> chunks = chunker.chunk(resource);
                for (TextChunk chunk : chunks) {
                    contexts.add(new ChunkContext(resource, chunk));
                }
            }
            if (contexts.isEmpty()) {
                jobRepository.markCompleted(jobId, documentsCount, 0);
                return jobRepository.findById(jobId);
            }

            int batchSize = DEFAULT_BATCH_SIZE;
            for (int start = 0; start < contexts.size(); start += batchSize) {
                int end = Math.min(start + batchSize, contexts.size());
                List<ChunkContext> batch = contexts.subList(start, end);
                List<String> inputs = batch.stream().map(c -> c.chunk.content()).toList();
                List<float[]> embeddings;
                try {
                    embeddings = embeddingClient.embed(inputs);
                } catch (IllegalStateException ex) {
                    throw new IllegalStateException(ex.getMessage(), ex);
                } catch (RuntimeException ex) {
                    throw new IllegalStateException("Embedding request failed: " + ex.getMessage(), ex);
                }
                if (CollectionUtils.isEmpty(embeddings)) {
                    log.warn("Embedding batch returned no vectors ({} inputs)", inputs.size());
                    continue;
                }
                if (embeddings.size() != batch.size()) {
                    log.warn("Embedding batch size mismatch: expected {}, got {}", batch.size(), embeddings.size());
                }
                int limit = Math.min(batch.size(), embeddings.size());
                for (int i = 0; i < limit; i++) {
                    ChunkContext ctx = batch.get(i);
                    chunkWriter.upsert(ctx.resource, ctx.chunk, embeddings.get(i));
                    stored++;
                }
            }
            log.info("RAG ingestion job (trigger={}) completed: documents={}, chunks={}", trigger, documentsCount, stored);
            jobRepository.markCompleted(jobId, documentsCount, stored);
            return jobRepository.findById(jobId);
        } catch (IllegalStateException ex) {
            if (jobId != null) {
                jobRepository.markFailed(jobId, ex.getMessage());
            }
            throw ex;
        } catch (Exception ex) {
            if (jobId != null) {
                jobRepository.markFailed(jobId, ex.getMessage());
            }
            log.error("RAG ingestion failed", ex);
            throw new IllegalStateException("RAG ingestion failed: " + ex.getMessage(), ex);
        } finally {
            running.set(false);
            log.info("RAG ingestion job (trigger={}) finished", trigger);
        }
    }

    private record ChunkContext(RagDocumentResource resource, TextChunk chunk) {
    }
}
