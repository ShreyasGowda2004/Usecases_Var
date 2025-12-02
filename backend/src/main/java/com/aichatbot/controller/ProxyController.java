package com.aichatbot.controller;

import com.aichatbot.dto.ProxyRequest;
import com.aichatbot.dto.ProxyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;

@RestController
@RequestMapping("/api/proxy")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:5173"}, allowCredentials = "true")
public class ProxyController {

    private static final Set<String> ALLOWED_METHODS = Set.of("GET","POST","PUT","PATCH","DELETE","HEAD","OPTIONS");

    @Value("${proxy.allowed.hosts:localhost,127.0.0.1}")
    private String allowedHostsProp;

    private Set<String> allowedHosts() {
        return Arrays.stream(allowedHostsProp.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private HttpClient createHttpClient() {
        try {
            // Create a trust manager that accepts all certificates (for development/testing)
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            return HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(15))
                    .sslContext(sslContext)
                    .build();
        } catch (Exception e) {
            // Fallback to default client if SSL setup fails
            return HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> proxy(@RequestBody ProxyRequest request) {
        try {
            if (request.getMethod() == null || request.getUrl() == null) {
                return ResponseEntity.badRequest().body(Map.of("error","method and url are required"));
            }
            String method = request.getMethod().toUpperCase(Locale.ROOT);
            if (!ALLOWED_METHODS.contains(method)) {
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                        .body(Map.of("error","HTTP method not allowed"));
            }
            URI target = URI.create(request.getUrl());
            Set<String> allowed = allowedHosts();
            if (!allowed.contains("*") && !allowed.contains(target.getHost())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error","Host not allowed: " + target.getHost()));
            }

            HttpClient client = createHttpClient();

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(target)
                    .timeout(Duration.ofSeconds(60));

            // Body publishing
            String body = request.getBody();
            boolean hasBody = body != null && !body.isEmpty() && List.of("POST","PUT","PATCH","DELETE").contains(method);
            if (hasBody) {
                builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            // Headers (basic whitelist to avoid host spoofing)
            Map<String,String> headers = request.getHeaders();
            if (headers != null) {
                headers.forEach((k,v) -> {
                    if (k == null || v == null) return;
                    String lk = k.toLowerCase(Locale.ROOT);
                    if (lk.equals("host") || lk.startsWith("sec-")) return; // skip sensitive / forbidden
                    builder.header(k, v);
                });
            }

            HttpResponse<String> resp = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            Map<String,String> responseHeaders = resp.headers().map().entrySet().stream()
                    .filter(e -> !e.getValue().isEmpty())
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));

            int status = resp.statusCode();
            String statusText = HttpStatus.resolve(status) != null ? HttpStatus.resolve(status).getReasonPhrase() : "";
            ProxyResponse pr = new ProxyResponse(status, statusText, responseHeaders, resp.body());
            return ResponseEntity.status(status).body(pr);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error","Proxy request failed","details", ex.getMessage()));
        }
    }
}
