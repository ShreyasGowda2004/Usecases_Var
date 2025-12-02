package com.aichatbot.repository;

import com.aichatbot.model.DocumentEmbedding;

import java.util.Collection;
import java.util.List;

/**
 * Abstraction for storing and retrieving embeddings without coupling to a specific DB.
 */
public interface EmbeddingStore {
    List<DocumentEmbedding> findAll();

    void save(DocumentEmbedding embedding);

    default void saveAll(Collection<DocumentEmbedding> embeddings) {
        if (embeddings == null) return;
        for (DocumentEmbedding e : embeddings) {
            save(e);
        }
    }

    void deleteByRepositoryOwnerAndRepositoryName(String repositoryOwner, String repositoryName);

    /** Stats helpers */
    default long count() {
        List<DocumentEmbedding> all = findAll();
        return all == null ? 0 : all.size();
    }

    /**
     * Return size on disk in bytes if known, or -1 if not applicable.
     */
    default long sizeOnDiskBytes() { return -1; }
}
