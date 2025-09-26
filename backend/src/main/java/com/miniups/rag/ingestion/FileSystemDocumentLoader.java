package com.miniups.rag.ingestion;

import com.miniups.rag.config.RagProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.ingestion.enabled", havingValue = "true", matchIfMissing = true)
public class FileSystemDocumentLoader {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("txt", "md", "markdown", "json");

    private final RagProperties properties;

    public List<RagDocumentResource> loadDocuments() {
        List<RagDocumentResource> documents = new ArrayList<>();
        for (String root : properties.getIngestion().getRootPaths()) {
            Path rootPath = resolvePath(root);
            if (rootPath == null) {
                continue;
            }
            try {
                Files.walk(rootPath)
                    .filter(Files::isRegularFile)
                    .filter(this::isSupported)
                    .forEach(path -> readDocument(documents, rootPath, path));
            } catch (IOException ex) {
                log.warn("Failed to walk root path {} for RAG ingestion", rootPath, ex);
            }
        }
        log.info("Loaded {} documents for RAG ingestion", documents.size());
        return documents;
    }

    private void readDocument(List<RagDocumentResource> documents, Path rootPath, Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String relative = rootPath.relativize(path).toString();
            String source = rootPath.getFileName() != null
                ? rootPath.getFileName().toString() + "/" + relative
                : relative;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("path", path.toString());
            metadata.put("relativePath", relative);
            metadata.put("root", rootPath.toString());
            metadata.put("ingestedAt", Instant.now().toString());
            String title = path.getFileName().toString();
            String documentId = UUID.nameUUIDFromBytes(path.toString().getBytes(StandardCharsets.UTF_8)).toString();
            documents.add(new RagDocumentResource(documentId, source, title, content, metadata, path));
        } catch (IOException ex) {
            log.warn("Failed to read document {} for RAG ingestion", path, ex);
        }
    }

    private boolean isSupported(Path path) {
        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        String ext = filename.substring(dot + 1).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(ext);
    }

    private Path resolvePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Path path = Paths.get(raw).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            log.warn("RAG ingestion root path {} does not exist", path);
            return null;
        }
        if (!Files.isDirectory(path)) {
            log.warn("RAG ingestion root path {} is not a directory", path);
            return null;
        }
        return path;
    }
}
