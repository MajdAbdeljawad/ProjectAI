package com.example.backend_meal.ai;

import com.example.backend_meal.nutrition.model.NutritionProfile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Provider Google Gemini.
 * Miroir de GeminiProvider dans src/server/ai.service.ts
 *
 * La clé API est injectée depuis AIProperties (jamais hardcodée).
 */
public class GeminiProvider implements IAIProvider {

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiProvider(String apiKey, String apiUrl, String model) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl.replaceAll("/+$", "");
        this.model = model;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "Google Gemini (" + model + ")";
    }

    @Override
    public boolean isRealAI() {
        return true;
    }

    @Override
    public String generateMealPlan(String prompt, NutritionProfile profile) throws Exception {
        String endpoint = apiUrl + "/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))
            ),
            "generationConfig", Map.of("responseMimeType", "application/json")
        );

        String requestBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error [" + response.statusCode() + "]: " + response.body());
        }

        Map<?, ?> responseMap = objectMapper.readValue(response.body(), Map.class);
        List<?> candidates = (List<?>) responseMap.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("Réponse vide reçue de Google Gemini.");
        }
        Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
        List<?> parts = (List<?>) content.get("parts");
        String text = (String) ((Map<?, ?>) parts.get(0)).get("text");

        if (text == null || text.isBlank()) {
            throw new RuntimeException("Réponse vide reçue de Google Gemini (text null).");
        }

        return text;
    }
}
