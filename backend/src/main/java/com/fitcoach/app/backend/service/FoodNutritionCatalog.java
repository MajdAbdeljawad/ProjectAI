package com.fitcoach.app.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class FoodNutritionCatalog {

    static final double DEFAULT_QUANTITY_G = 100.0;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, FoodEntry> byAlias = new HashMap<>();
    private final List<FoodEntry> foods = new ArrayList<>();

    @PostConstruct
    void load() throws Exception {
        try (InputStream in = new ClassPathResource("nutrition/foods.json").getInputStream()) {
            List<FoodEntry> loaded = objectMapper.readValue(in, new TypeReference<>() {});
            foods.clear();
            foods.addAll(loaded);
            byAlias.clear();
            for (FoodEntry food : loaded) {
                byAlias.put(normalize(food.getId().replace('_', ' ')), food);
                for (String alias : food.getAliases()) {
                    byAlias.put(normalize(alias), food);
                }
            }
        }
    }

    public int size() {
        return foods.size();
    }

    public Optional<FoodEntry> find(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }
        String key = normalize(rawName);
        FoodEntry exact = byAlias.get(key);
        if (exact != null) {
            return Optional.of(exact);
        }
        return byAlias.entrySet().stream()
                .filter(e -> key.contains(e.getKey()) || e.getKey().contains(key))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public boolean isCompatible(FoodEntry food, String dietType) {
        if (dietType == null || dietType.isBlank()) {
            return true;
        }
        return food.getDiets().stream().anyMatch(d -> d.equalsIgnoreCase(dietType.trim()));
    }

    static String normalize(String value) {
        String lower = value.toLowerCase(Locale.ROOT).trim();
        String noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return noAccents.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    @Data
    public static class FoodEntry {
        private String id;
        private List<String> aliases = new ArrayList<>();
        private String category;
        private int kcal;
        private double protein;
        private double carbs;
        private double fat;
        private List<String> diets = new ArrayList<>();
    }
}
