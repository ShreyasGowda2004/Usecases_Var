package com.aichatbot.model;

import java.time.LocalDateTime;

/**
 * Document Embedding Model (Text Chunk Storage).
 * 
 * IMPORTANT CLARIFICATION: Despite the class name "DocumentEmbedding",
 * this model does NOT store vector embeddings or neural representations.
 * It stores plain text chunks from documents for keyword-based search.
 * 
 * Purpose:
 * - Store document text chunks split from larger files
 * - Track metadata for source attribution
 * - Enable efficient keyword-based retrieval
 * - Support repository and branch tracking
 * 
 * Data Structure:
 * - id: Unique identifier for the chunk
 * - filePath: Original file path in repository
 * - repositoryOwner: GitHub organization/user
 * - repositoryName: Repository name
 * - branchName: Git branch (main, develop, etc.)
 * - contentChunk: The actual text content (2000 chars max)
 * - chunkIndex: Sequential number for chunk ordering
 * - fileHash: Hash for detecting file changes
 * - createdAt/updatedAt: Timestamps for tracking
 * 
 * Storage Format:
 * Persisted as JSON Lines in: data/embeddings/embeddings.jsonl
 * Example: {"id":"doc-001","filePath":"README.md","contentChunk":"...","chunkIndex":0,...}
 * 
 * Search Method:
 * Chunks are searched using keyword matching, NOT vector similarity.
 * The system calculates relevance scores based on:
 * - Keyword frequency in content
 * - Filename matches
 * - Term variations and synonyms
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
public class DocumentEmbedding {
    
    private String id;

    private String filePath;

    private String repositoryOwner;

    private String repositoryName;

    private String branchName;

    private String contentChunk;

    private Integer chunkIndex;

    private String fileHash;

    private String embeddingId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Lifecycle helpers
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }
    
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public DocumentEmbedding() {}
    
    public DocumentEmbedding(String filePath, String repositoryOwner, String repositoryName, 
                           String branchName, String contentChunk, Integer chunkIndex) {
        this.filePath = filePath;
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.branchName = branchName;
        this.contentChunk = contentChunk;
        this.chunkIndex = chunkIndex;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getRepositoryOwner() { return repositoryOwner; }
    public void setRepositoryOwner(String repositoryOwner) { this.repositoryOwner = repositoryOwner; }
    
    public String getRepositoryName() { return repositoryName; }
    public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }
    
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    
    public String getContentChunk() { return contentChunk; }
    public void setContentChunk(String contentChunk) { this.contentChunk = contentChunk; }
    
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    
    public String getEmbeddingId() { return embeddingId; }
    public void setEmbeddingId(String embeddingId) { this.embeddingId = embeddingId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
