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
 * Provider OpenAI Compatible — fonctionne avec OpenAI, Groq, Ollama, etc.
 * Miroir de OpenAICompatibleProvider dans src/server/ai.service.ts
 *
 * La clé API est injectée depuis AIProperties (jamais hardcodée).
 */
public class OpenAICompatibleProvider implements IAIProvider {

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAICompatibleProvider(String apiKey, String apiUrl, String model) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl.replaceAll("/+$", "");
        this.model = model;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "OpenAI Compatible (" + model + " @ " + apiUrl + ")";
    }

    @Override
    public boolean isRealAI() {
        return true;
    }

    @Override
    public String generateMealPlan(String prompt, NutritionProfile profile) throws Exception {
        String endpoint = apiUrl + "/chat/completions";

        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content",
                    "Tu es un expert diététicien-nutritionniste du sport de renommée mondiale. "
                    + "Tu réponds UNIQUEMENT en JSON valide strict conforme au schéma demandé. "
                    + "Pas de markdown, pas de texte avant ou après."),
                Map.of("role", "user", "content", prompt)
            ),
            "response_format", Map.of("type", "json_object"),
            "temperature", 0.7
        );

        String requestBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API error [" + response.statusCode() + "]: " + response.body());
        }

        // Extraire choices[0].message.content
        Map<?, ?> responseMap = objectMapper.readValue(response.body(), Map.class);
        List<?> choices = (List<?>) responseMap.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("Réponse vide reçue du modèle IA (aucun choix).");
        }
        Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
        String content = (String) message.get("content");

        if (content == null || content.isBlank()) {
            throw new RuntimeException("Réponse vide reçue du modèle IA (content null).");
        }

        return content;
    }
}
