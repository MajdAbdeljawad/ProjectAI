package com.fitcoach.app.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RecoveryAiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String aiApiKey;
    private final String aiProvider;
    private final String aiEndpoint;
    private final String aiModel;

    public RecoveryAiClient(
            ObjectMapper objectMapper,
            @Value("${ai.api-key:}") String aiApiKey,
            @Value("${ai.provider:ollama}") String aiProvider,
            @Value("${ai.endpoint:http://localhost:11434/v1/chat/completions}") String aiEndpoint,
            @Value("${ai.model:llama3.2:1b}") String aiModel,
            @Value("${ai.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${ai.read-timeout-ms:45000}") int readTimeoutMs
    ) {
        this.objectMapper = objectMapper;
        this.aiApiKey = aiApiKey;
        this.aiProvider = aiProvider;
        this.aiEndpoint = aiEndpoint;
        this.aiModel = aiModel;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restTemplate = new RestTemplate(factory);
    }

    public String complete(String systemMessage, String userMessage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (aiApiKey != null && !aiApiKey.isBlank()) {
                headers.setBearerAuth(aiApiKey);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", aiModel);
            payload.put("temperature", 0.2);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", systemMessage),
                    Map.of("role", "user", "content", userMessage)
            ));
            if ("openai".equalsIgnoreCase(aiProvider)) {
                payload.put("response_format", Map.of("type", "json_object"));
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(aiEndpoint, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RecoveryProviderException("The recovery model did not return a usable response.", "UNAVAILABLE");
            }
            return extractContent(response.getBody());
        } catch (RecoveryProviderException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new RecoveryProviderException("The recovery model timed out or could not be reached.", "TIMEOUT", e);
        } catch (RestClientException e) {
            throw new RecoveryProviderException("The recovery model is currently unavailable.", "UNAVAILABLE", e);
        } catch (Exception e) {
            throw new RecoveryProviderException("The recovery model returned an unreadable response.", "MALFORMED_RESPONSE", e);
        }
    }

    String extractContent(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new RecoveryProviderException("The recovery model response was missing message content.", "MALFORMED_RESPONSE");
            }
            String text = content.isTextual() ? content.asText() : content.toString();
            if (text == null || text.isBlank()) {
                throw new RecoveryProviderException("The recovery model returned an empty message.", "MALFORMED_RESPONSE");
            }
            return text;
        } catch (RecoveryProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new RecoveryProviderException("The recovery model returned malformed JSON.", "MALFORMED_RESPONSE", e);
        }
    }
}
