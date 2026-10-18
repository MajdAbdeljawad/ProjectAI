package com.example.backend_meal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration CORS globale — autorise le frontend Angular à appeler l'API Spring Boot.
 * La clé API n'est jamais transmise au frontend.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AIProperties aiProperties;

    public WebConfig(AIProperties aiProperties) {
        this.aiProperties = aiProperties;
        logAIStatus();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200", "http://localhost:4000")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    private void logAIStatus() {
        if (aiProperties.hasRealKey()) {
            String provider = aiProperties.isGemini() ? "Google Gemini" : "OpenAI Compatible";
            System.out.println("[AIService] ✅ Fournisseur IA RÉEL : " + provider
                    + " | Modèle : " + aiProperties.getModel()
                    + " | URL : " + aiProperties.getApiUrl());
        } else {
            System.out.println("[AIService] ⚠️  Aucune clé AI_API_KEY dans l'environnement. "
                    + "Le moteur algorithmique de DÉVELOPPEMENT est activé. "
                    + "Ce moteur n'est PAS une IA générative.");
        }
    }
}
