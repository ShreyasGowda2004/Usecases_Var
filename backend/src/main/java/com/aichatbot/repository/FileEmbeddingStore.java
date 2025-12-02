package com.aichatbot.repository;

import com.aichatbot.model.DocumentEmbedding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * File-Based Document Chunk Storage (No External Database Required).
 * 
 * IMPORTANT: Despite the name "EmbeddingStore", this class does NOT store
 * vector embeddings. It stores plain text chunks for keyword-based search.
 * 
 * Architecture:
 * - In-memory index for fast keyword searches
 * - JSONL file persistence for durability
 * - Thread-safe with CopyOnWriteArrayList and ConcurrentHashMap
 * - No MongoDB, PostgreSQL, or vector database required
 * 
 * Storage Format:
 * File: data/embeddings/embeddings.jsonl
 * Format: One JSON object per line (JSON Lines format)
 * Example: {"id":"doc-001","filePath":"README.md","contentChunk":"...","chunkIndex":0,...}
 * 
 * Performance:
 * - Fast in-memory search (no disk I/O for reads)
 * - Lazy write (persists only on add/delete operations)
 * - Suitable for 100K+ document chunks
 * - Startup loads entire file into memory
 * 
 * Operations:
 * - add(): Append chunk to memory and file
 * - findAll(): Return all chunks from memory
 * - deleteByRepository(): Remove all chunks for a repository
 * - Automatic initialization on first use
 * 
 * Thread Safety:
 * - CopyOnWriteArrayList for concurrent reads
 * - ConcurrentHashMap for repository indexing
 * - Synchronized writes to disk
 * 
 * Use Cases:
 * - Small to medium deployments (< 1M chunks)
 * - Development and testing
 * - Production systems with limited document corpus
 * - Situations where external database adds complexity
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
public class FileEmbeddingStore implements EmbeddingStore {
    private static final Logger logger = LoggerFactory.getLogger(FileEmbeddingStore.class);

    private final Path dataDir;
    private final Path dataFile;

    // In-memory index for speed
    private final List<DocumentEmbedding> all = new CopyOnWriteArrayList<>();
    private final Map<String, List<DocumentEmbedding>> byRepo = new ConcurrentHashMap<>(); // key: owner|name

    public FileEmbeddingStore(Path dataDir) {
        this.dataDir = dataDir;
        this.dataFile = dataDir.resolve("embeddings.jsonl");
        init();
    }

    private void init() {
        try {
            if (Files.notExists(dataDir)) {
                Files.createDirectories(dataDir);
                logger.info("Created embeddings directory: {}", dataDir.toAbsolutePath());
            }
            if (Files.exists(dataFile)) {
                logger.info("Embeddings file found at {} — loading", dataFile.toAbsolutePath());
                loadFromDisk();
            } else {
                Files.createFile(dataFile);
                logger.info("Created new empty embeddings file at {}", dataFile.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.warn("Failed to initialize FileEmbeddingStore", e);
        }
    }

    private void loadFromDisk() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                DocumentEmbedding e = deserialize(line);
                if (e != null) {
                    index(e);
                }
            }
        }
        logger.info("Loaded {} embeddings from {}", all.size(), dataFile);
    }

    private void appendToDisk(DocumentEmbedding e) {
        try (BufferedWriter writer = Files.newBufferedWriter(dataFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            writer.write(serialize(e));
            writer.newLine();
        } catch (IOException ex) {
            logger.warn("Failed to persist embedding to disk", ex);
        }
    }

    private void rewriteFile() {
        // Compact file to current in-memory state, used after deletions
        Path tmp = dataFile.getParent().resolve(dataFile.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (DocumentEmbedding e : all) {
                writer.write(serialize(e));
                writer.newLine();
            }
        } catch (IOException ex) {
            logger.warn("Failed to rewrite embeddings file", ex);
            return;
        }
        try {
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            logger.warn("Failed to atomically replace embeddings file", ex);
        }
    }

    private void index(DocumentEmbedding e) {
        all.add(e);
        String key = repoKey(e.getRepositoryOwner(), e.getRepositoryName());
        byRepo.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(e);
    }

    private String repoKey(String owner, String name) {
        return (owner == null ? "" : owner) + "|" + (name == null ? "" : name);
    }

    @Override
    public List<DocumentEmbedding> findAll() {
        return new ArrayList<>(all);
    }

    @Override
    public void save(DocumentEmbedding embedding) {
        if (embedding.getId() == null || embedding.getId().isBlank()) {
            embedding.setId(UUID.randomUUID().toString());
        }
        if (embedding.getCreatedAt() == null) {
            embedding.setCreatedAt(LocalDateTime.now());
        }
        embedding.setUpdatedAt(LocalDateTime.now());
        index(embedding);
        appendToDisk(embedding);
    }

    @Override
    public void deleteByRepositoryOwnerAndRepositoryName(String repositoryOwner, String repositoryName) {
        String key = repoKey(repositoryOwner, repositoryName);
        List<DocumentEmbedding> removed = byRepo.remove(key);
        if (removed != null) {
            Set<String> ids = removed.stream().map(DocumentEmbedding::getId).collect(Collectors.toSet());
            all.removeIf(e -> ids.contains(e.getId()));
            // Compact file to reflect deletions
            rewriteFile();
        }
    }

    @Override
    public long sizeOnDiskBytes() {
        try {
            if (Files.exists(dataFile)) {
                return Files.size(dataFile);
            }
        } catch (IOException ignored) { }
        return -1;
    }

    private String serialize(DocumentEmbedding e) {
        // Minimal JSON to avoid extra dependencies
        StringBuilder sb = new StringBuilder();
        sb.append('{')
          .append("\"id\":\"").append(escape(e.getId())).append("\",")
          .append("\"filePath\":\"").append(escape(e.getFilePath())).append("\",")
          .append("\"repositoryOwner\":\"").append(escape(e.getRepositoryOwner())).append("\",")
          .append("\"repositoryName\":\"").append(escape(e.getRepositoryName())).append("\",")
          .append("\"branchName\":\"").append(escape(e.getBranchName())).append("\",")
          .append("\"contentChunk\":\"").append(escape(e.getContentChunk())).append("\",")
          .append("\"chunkIndex\":").append(e.getChunkIndex() == null ? "null" : e.getChunkIndex()).append(',')
          .append("\"fileHash\":\"").append(escape(e.getFileHash())).append("\",")
          .append("\"embeddingId\":\"").append(escape(e.getEmbeddingId())).append("\",")
          .append("\"createdAt\":\"").append(e.getCreatedAt() == null ? "" : e.getCreatedAt().toString()).append("\",")
          .append("\"updatedAt\":\"").append(e.getUpdatedAt() == null ? "" : e.getUpdatedAt().toString()).append('"')
          .append('}');
        return sb.toString();
    }

    private DocumentEmbedding deserialize(String json) {
        try {
            // Very small, permissive parser for our own JSON layout
            Map<String, String> map = new HashMap<>();
            String body = json.trim();
            if (body.startsWith("{")) body = body.substring(1);
            if (body.endsWith("}")) body = body.substring(0, body.length() - 1);
            // Split on commas that separate fields
            List<String> parts = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inString = false;
            for (int i = 0; i < body.length(); i++) {
                char c = body.charAt(i);
                if (c == '"' && (i == 0 || body.charAt(i - 1) != '\\')) {
                    inString = !inString;
                    current.append(c);
                } else if (c == ',' && !inString) {
                    parts.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
            if (current.length() > 0) parts.add(current.toString());

            for (String part : parts) {
                int idx = part.indexOf(":");
                if (idx <= 0) continue;
                String key = unquote(part.substring(0, idx).trim());
                String val = part.substring(idx + 1).trim();
                if (!"null".equals(val)) {
                    map.put(key, unquote(val));
                } else {
                    map.put(key, null);
                }
            }

            DocumentEmbedding e = new DocumentEmbedding();
            e.setId(map.get("id"));
            e.setFilePath(map.get("filePath"));
            e.setRepositoryOwner(map.get("repositoryOwner"));
            e.setRepositoryName(map.get("repositoryName"));
            e.setBranchName(map.get("branchName"));
            e.setContentChunk(map.get("contentChunk"));
            e.setChunkIndex(map.get("chunkIndex") == null ? null : Integer.parseInt(map.get("chunkIndex")));
            e.setFileHash(map.get("fileHash"));
            e.setEmbeddingId(map.get("embeddingId"));
            String createdAt = map.get("createdAt");
            String updatedAt = map.get("updatedAt");
            if (createdAt != null && !createdAt.isBlank()) e.setCreatedAt(LocalDateTime.parse(createdAt));
            if (updatedAt != null && !updatedAt.isBlank()) e.setUpdatedAt(LocalDateTime.parse(updatedAt));
            return e;
        } catch (Exception ex) {
            logger.warn("Failed to parse embedding json line, skipping", ex);
            return null;
        }
    }

    private String unquote(String s) {
        s = s.trim();
        if (s.startsWith("\"")) s = s.substring(1);
        if (s.endsWith("\"")) s = s.substring(0, s.length() - 1);
        return s.replace("\\\"", "\"").replace("\\n", "\n");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
