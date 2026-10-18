package com.example.backend_meal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Lit les propriétés ai.* depuis application.properties,
 * elles-mêmes sourcées depuis des variables d'environnement OS.
 *
 * Mécanisme : application.properties  ←  OS env vars (AI_API_KEY, etc.)
 *
 * La clé API n'est JAMAIS présente en dur dans le code source.
 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AIProperties {

    /** 'openai' | 'gemini' | 'dynamic' (fallback algorithmique) */
    private String provider = "dynamic";

    /** URL de base de l'API IA (ex: https://api.openai.com/v1) */
    private String apiUrl = "https://api.openai.com/v1";

    /** Clé API secrète — injectée depuis la variable d'env AI_API_KEY */
    private String apiKey = "";

    /** Nom du modèle (ex: gpt-4o-mini, gemini-1.5-flash) */
    private String model = "gpt-4o-mini";

    // ---- Getters / Setters (pas de Lombok pour la lisibilité) ----

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    /** @return true si une vraie clé API non vide est configurée */
    public boolean hasRealKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** @return true si le fournisseur est OpenAI ou compatible */
    public boolean isOpenAI() {
        return hasRealKey() && !"gemini".equalsIgnoreCase(provider) && !apiKey.startsWith("AIza");
    }

    /** @return true si le fournisseur est Google Gemini */
    public boolean isGemini() {
        return hasRealKey() && ("gemini".equalsIgnoreCase(provider) || apiKey.startsWith("AIza"));
    }
}
