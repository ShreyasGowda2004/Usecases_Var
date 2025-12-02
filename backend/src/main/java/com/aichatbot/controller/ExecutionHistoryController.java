package com.aichatbot.controller;

import com.aichatbot.model.ExecutionHistory;
import com.aichatbot.service.ExecutionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Execution History Controller for API Request/Response Tracking.
 * 
 * This REST controller manages the history of API executions performed
 * through various interfaces (Console, Chat, GitHub file viewer).
 * 
 * Key Features:
 * - Save API execution records with full request/response details
 * - Retrieve user-specific execution history
 * - Get individual execution records for replay
 * - Delete execution records
 * - Support for multiple execution sources
 * 
 * Execution Sources:
 * - Execution Console: Manual API testing interface
 * - Chat Play Button: Auto-extracted API calls from AI responses
 * - GitHub File View: Execute API sections from documentation
 * 
 * Endpoints:
 * - POST /execution-history: Save a new execution record
 * - GET /execution-history/{username}: Get user's execution history
 * - GET /execution-history/detail/{id}: Get specific execution details
 * - DELETE /execution-history/{id}: Delete an execution record
 * 
 * Stored Information:
 * - Complete HTTP request (method, URL, headers, body)
 * - Complete HTTP response (status, headers, body)
 * - Execution metadata (username, timestamp, source)
 * 
 * Use Cases:
 * - View API execution history
 * - Replay previous API requests
 * - Debug API interactions
 * - Audit API usage
 * - Learn from successful API calls
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/execution-history")
@CrossOrigin(origins = "*")
public class ExecutionHistoryController {

	@Autowired
	private ExecutionHistoryService executionHistoryService;

	@PostMapping
	public ResponseEntity<?> save(@RequestBody ExecutionHistory executionHistory) {
		try {
			ExecutionHistory saved = executionHistoryService.save(executionHistory);
			return ResponseEntity.ok(saved);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body(Map.of("error", e.toString()));
		}
	}

	@GetMapping("/{username}")
	public ResponseEntity<?> listByUser(@PathVariable String username) {
		try {
			List<ExecutionHistory> list = executionHistoryService.listByUsername(username);
			return ResponseEntity.ok(list);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body(Map.of("error", e.toString()));
		}
	}

	@GetMapping("/count/{username}")
	public ResponseEntity<?> countByUser(@PathVariable String username) {
		try {
			long count = executionHistoryService.countByUsername(username);
			return ResponseEntity.ok(Map.of("count", count));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body(Map.of("error", e.toString()));
		}
	}

	@GetMapping("/id/{id}")
	public ResponseEntity<?> getById(@PathVariable String id) {
		try {
			Optional<ExecutionHistory> opt = executionHistoryService.getById(id);
			return opt.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body(Map.of("error", e.toString()));
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteById(@PathVariable String id) {
		try {
			executionHistoryService.deleteById(id);
			return ResponseEntity.ok(Map.of("message", "Deleted"));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body(Map.of("error", e.toString()));
		}
	}

	@DeleteMapping("/user/{username}")
	public ResponseEntity<?> deleteAllForUser(@PathVariable String username) {
		try {
			executionHistoryService.deleteByUsername(username);
			return ResponseEntity.ok(Map.of("message", "All history deleted"));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body(Map.of("error", e.toString()));
		}
	}
}
