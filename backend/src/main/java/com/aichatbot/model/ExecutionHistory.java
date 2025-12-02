package com.aichatbot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.annotation.Transient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ExecutionHistory captures a single API execution initiated either from:
 *  - Execution Console (manual request)
 *  - Chat Play button (auto-extracted request from model output)
 *  - GitHub file view (section execute)
 *
 * Each record stores full request + response metadata for observability and re-run.
 */
@Document(collection = "execution_history")
@CompoundIndex(name = "user_time_idx", def = "{ 'username': 1, 'timestamp': -1 }")
public class ExecutionHistory {

	@Id
	private String id;

	@Indexed
	private String username;            // Owning user
	private Instant timestamp;          // When execution occurred (UTC)
	private String source;              // console | chat | github-section
	private String actionTitle;         // e.g. "Create Organization" or "Manual Request"
	private String instanceId;          // Saved instance id (if execution used a saved instance)

	// Request details
	private String method;              // HTTP method
	private String url;                 // Fully resolved URL
	// Store raw as Object to be tolerant of historical shapes (Map or List)
	@JsonIgnore
	@Field("requestHeaders")
	private Object requestHeadersRaw; // could be List<Map>, List<KeyValue>, Map, or null

	@JsonIgnore
	@Field("requestParams")
	private Object requestParamsRaw;  // could be List<Map>, List<KeyValue>, Map, or null
	private String requestBody;         // Raw body text (possibly JSON)

	// Result details
	private String status;              // success | error
	private Integer statusCode;         // HTTP status from target (if available)
	private Long durationMs;            // Round-trip time
	@JsonIgnore
	@Field("responseHeaders")
	private Object responseHeadersRaw; // Response headers (Map or List)
	private String responseBody;        // Body (possibly truncated client-side)
	private String errorMessage;        // Error summary when status=error
	private String stackTrace;          // Stack trace if available (proxy/client errors)

	public ExecutionHistory() {
		this.timestamp = Instant.now();
		this.requestHeadersRaw = new ArrayList<>();
		this.requestParamsRaw = new ArrayList<>();
		this.responseHeadersRaw = new ArrayList<>();
	}

	// Convenience constructor for minimal fields
	public ExecutionHistory(String username, String source, String actionTitle) {
		this();
		this.username = username;
		this.source = source;
		this.actionTitle = actionTitle;
	}

	// Nested key/value pair representation
	public static class KeyValue {
		private String key;
		private String value;

		public KeyValue() {}

		public KeyValue(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() { return key; }
		public void setKey(String key) { this.key = key; }
		public String getValue() { return value; }
		public void setValue(String value) { this.value = value; }
	}

	// Getters / Setters
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }
	public Instant getTimestamp() { return timestamp; }
	public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
	public String getSource() { return source; }
	public void setSource(String source) { this.source = source; }
	public String getActionTitle() { return actionTitle; }
	public void setActionTitle(String actionTitle) { this.actionTitle = actionTitle; }
	public String getInstanceId() { return instanceId; }
	public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
	public String getMethod() { return method; }
	public void setMethod(String method) { this.method = method; }
	public String getUrl() { return url; }
	public void setUrl(String url) { this.url = url; }

	// Normalized JSON-facing getters/setters for headers/params that adapt to various Mongo shapes
	@JsonProperty("requestHeaders")
	@Transient
	public List<KeyValue> getRequestHeaders() { return normalizeKVList(this.requestHeadersRaw); }
	@JsonProperty("requestHeaders")
	@Transient
	public void setRequestHeaders(List<KeyValue> requestHeaders) { this.requestHeadersRaw = safeFromKVList(requestHeaders); }

	@JsonProperty("requestParams")
	@Transient
	public List<KeyValue> getRequestParams() { return normalizeKVList(this.requestParamsRaw); }
	@JsonProperty("requestParams")
	@Transient
	public void setRequestParams(List<KeyValue> requestParams) { this.requestParamsRaw = safeFromKVList(requestParams); }
	public String getRequestBody() { return requestBody; }
	public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public Integer getStatusCode() { return statusCode; }
	public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
	public Long getDurationMs() { return durationMs; }
	public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

	@JsonProperty("responseHeaders")
	@Transient
	public List<KeyValue> getResponseHeaders() { return normalizeKVList(this.responseHeadersRaw); }
	@JsonProperty("responseHeaders")
	@Transient
	public void setResponseHeaders(List<KeyValue> responseHeaders) { this.responseHeadersRaw = safeFromKVList(responseHeaders); }
	public String getResponseBody() { return responseBody; }
	public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
	public String getErrorMessage() { return errorMessage; }
	public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
	public String getStackTrace() { return stackTrace; }
	public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

	// Raw accessors (not serialized)
	@JsonIgnore
	public Object getRequestHeadersRaw() { return requestHeadersRaw; }
	@JsonIgnore
	public void setRequestHeadersRaw(Object requestHeadersRaw) { this.requestHeadersRaw = requestHeadersRaw; }
	@JsonIgnore
	public Object getRequestParamsRaw() { return requestParamsRaw; }
	@JsonIgnore
	public void setRequestParamsRaw(Object requestParamsRaw) { this.requestParamsRaw = requestParamsRaw; }
	@JsonIgnore
	public Object getResponseHeadersRaw() { return responseHeadersRaw; }
	@JsonIgnore
	public void setResponseHeadersRaw(Object responseHeadersRaw) { this.responseHeadersRaw = responseHeadersRaw; }

	// Helpers to normalize various Mongo shapes to a List<KeyValue>
	@Transient
	private List<KeyValue> normalizeKVList(Object raw) {
		List<KeyValue> out = new ArrayList<>();
		if (raw == null) return out;
		if (raw instanceof List<?> list) {
			for (Object o : list) {
				if (o instanceof KeyValue kv) {
					out.add(new KeyValue(kv.getKey(), kv.getValue()));
				} else if (o instanceof Map<?, ?> m) {
					Object k = m.get("key");
					Object v = m.get("value");
					if (k == null && m.size() == 1) {
						// shape like [{"Header-Name":"val"}]
						Map.Entry<?, ?> e = m.entrySet().iterator().next();
						k = e.getKey();
						v = e.getValue();
					}
					out.add(new KeyValue(Objects.toString(k, null), v == null ? null : String.valueOf(v)));
				} else if (o != null) {
					out.add(new KeyValue(String.valueOf(o), null));
				}
			}
			return out;
		}
		if (raw instanceof Map<?, ?> map) {
			for (Map.Entry<?, ?> e : map.entrySet()) {
				String k = e.getKey() == null ? null : String.valueOf(e.getKey());
				Object v = e.getValue();
				if (v instanceof String s) {
					out.add(new KeyValue(k, s));
				} else if (v instanceof List<?> l) {
					out.add(new KeyValue(k, String.valueOf(l)));
				} else if (v instanceof Map<?, ?> vm) {
					out.add(new KeyValue(k, String.valueOf(vm)));
				} else {
					out.add(new KeyValue(k, v == null ? null : String.valueOf(v)));
				}
			}
			return out;
		}
		// Fallback: treat as single value
		out.add(new KeyValue(String.valueOf(raw), null));
		return out;
	}

	@Transient
	private Object safeFromKVList(List<KeyValue> list) {
		if (list == null) return new ArrayList<>();
		List<Map<String, String>> out = new ArrayList<>();
		for (KeyValue kv : list) {
			out.add(Map.of(
				"key", kv.getKey(),
				"value", kv.getValue()
			));
		}
		return out;
	}
}
