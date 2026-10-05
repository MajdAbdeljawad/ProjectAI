package com.fitcoach.app.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.entity.UserProfile;
import com.fitcoach.app.backend.entity.WorkoutDay;
import com.fitcoach.app.backend.entity.WorkoutExercise;
import com.fitcoach.app.backend.entity.WorkoutPlan;
import com.fitcoach.app.backend.entity.WorkoutWeek;
import com.fitcoach.app.backend.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiPlanService {

    private final WorkoutPlanRepository workoutPlanRepository;
    private final AiPromptService aiPromptService;
    private final AiResponseParser aiResponseParser;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.api-key:demo-key}")
    private String aiApiKey;

    @Value("${ai.provider:ollama}")
    private String aiProvider;

    @Value("${ai.endpoint:https://api.openai.com/v1/chat/completions}")
    private String aiEndpoint;

    @Value("${ai.model:gpt-4o-mini}")
    private String aiModel;

    public WorkoutPlan generateForUser(User user, UserProfile profile) {
        return generate(user, profile, aiPromptService.buildPlanPrompt(user, profile), true);
    }

    public WorkoutPlan generateGuestPlan(UserProfile profile, String injuriesOrLimitations) {
        return generate(null, profile, aiPromptService.buildPlanPrompt(profile, injuriesOrLimitations), false);
    }

    private WorkoutPlan generate(User user, UserProfile profile, String prompt, boolean persist) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (aiApiKey != null && !aiApiKey.isBlank()) {
                headers.setBearerAuth(aiApiKey);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", aiModel);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", "You generate safe, structured fitness plans as JSON."),
                    Map.of("role", "user", "content", prompt)
            ));
            if ("openai".equalsIgnoreCase(aiProvider)) {
                payload.put("response_format", Map.of("type", "json_object"));
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(aiEndpoint, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");
                WorkoutPlan plan = aiResponseParser.parse(content, user, profile);
                plan.setUser(user);
                return persist ? workoutPlanRepository.save(plan) : plan;
            }
        } catch (Exception e) {
            return buildFallbackPlan(user, persist);
        }

        return buildFallbackPlan(user, persist);
    }

    private WorkoutPlan buildFallbackPlan(User user, boolean persist) {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setUser(user);
        plan.setTitle("30-Day Foundation Plan");
        plan.setSummary("A balanced month focused on technique, consistency, and progressive overload.");
        plan.setWeeklyStrategy("Week 1 technique and adaptation, week 2 volume, week 3 progressive overload, week 4 evaluation and performance.");
        plan.setDurationDays(30);

        for (int weekIndex = 1; weekIndex <= 4; weekIndex++) {
            WorkoutWeek week = new WorkoutWeek();
            week.setWeekNumber(weekIndex);
            week.setPlan(plan);
            week.setFocus(switch (weekIndex) {
                case 1 -> "Technique and adaptation";
                case 2 -> "Increase volume";
                case 3 -> "Progressive overload";
                default -> "Performance and evaluation";
            });

            for (int day = 1; day <= 7; day++) {
                WorkoutDay workoutDay = new WorkoutDay();
                workoutDay.setDayNumber(day);
                workoutDay.setWeek(week);
                workoutDay.setType(day % 3 == 0 ? "REST" : "STRENGTH");
                workoutDay.setNotes(day % 3 == 0 ? "Recovery and mobility" : "Train with controlled effort");

                if (day % 3 != 0) {
                    WorkoutExercise ex = new WorkoutExercise();
                    ex.setName("Compound movement");
                    ex.setSets(3);
                    ex.setReps(8);
                    ex.setRestSeconds(90);
                    ex.setIntensity("Moderate");
                    ex.setNotes("Focus on controlled tempo");
                    ex.setDay(workoutDay);
                    workoutDay.setExercises(List.of(ex));
                }
                week.getDays().add(workoutDay);
            }
            plan.getWeeks().add(week);
        }
        return persist ? workoutPlanRepository.save(plan) : plan;
    }
}
