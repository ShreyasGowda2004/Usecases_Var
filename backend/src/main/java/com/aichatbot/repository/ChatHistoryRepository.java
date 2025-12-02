package com.aichatbot.repository;

import com.aichatbot.model.ChatHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatHistoryRepository extends MongoRepository<ChatHistory, String> {
    List<ChatHistory> findByUsernameOrderByCreatedAtDesc(String username);
    void deleteByUsername(String username);
    long countByUsername(String username);
}
