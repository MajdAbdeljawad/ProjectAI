package com.fitcoach.app.backend.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiCompletionClient {

    private final RestTemplate restTemplate;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Value("${ai.endpoint:http://localhost:11434/v1/chat/completions}")
    private String aiEndpoint;

    @Getter
    @Value("${ai.model:llama3.2:1b}")
    private String aiModel;

    public AiCompletionClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(180_000);
        this.restTemplate = new RestTemplate(factory);
    }

    @SuppressWarnings("unchecked")
    public String completeJson(String systemPrompt, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (aiApiKey != null && !aiApiKey.isBlank()) {
            headers.setBearerAuth(aiApiKey);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", aiModel);
        payload.put("temperature", 0.4);
        payload.put("response_format", Map.of("type", "json_object"));
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));

        ResponseEntity<Map> response = restTemplate.postForEntity(aiEndpoint, new HttpEntity<>(payload, headers), Map.class);
        Map<String, Object> body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null) {
            throw new IllegalStateException("AI provider returned " + response.getStatusCode());
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
