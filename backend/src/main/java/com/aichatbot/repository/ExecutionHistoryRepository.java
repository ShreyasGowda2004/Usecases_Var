package com.aichatbot.repository;

import com.aichatbot.model.ExecutionHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionHistoryRepository extends MongoRepository<ExecutionHistory, String> {
	List<ExecutionHistory> findByUsernameOrderByTimestampDesc(String username);
	long countByUsername(String username);
	void deleteByUsername(String username);
}
