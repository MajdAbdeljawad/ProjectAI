package com.fitcoach.app.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.app.backend.dto.ProgressAnalysisRequest;
import com.fitcoach.app.backend.dto.ProgressAnalysisResponse;
import com.fitcoach.app.backend.dto.ProgressAnalysisResponse.ProgressStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressAnalysisService {

    private static final int REQUIRED_TIPS = 3;

    private final ProgressCalculationsService progressCalculationsService;
    private final ProgressPromptService progressPromptService;
    private final AiCompletionClient aiCompletionClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProgressAnalysisResponse analyze(ProgressAnalysisRequest request) {
        ProgressAnalysisResponse analysis = progressCalculationsService.analyze(request);
        analysis.setTitle("Progress report for " + request.getFirstName());

        String prompt = progressPromptService.buildProgressPrompt(request, analysis);
        analysis.setPrompt(prompt);
        analysis.setModel(aiCompletionClient.getAiModel());

        try {
            String content = aiCompletionClient.completeJson(ProgressPromptService.SYSTEM_PROMPT, prompt);
            applyCoachAdvice(analysis, parse(content));
            analysis.setSource("ai");
        } catch (Exception e) {
            log.warn("AI progress analysis unavailable, using fallback: {}", e.getMessage());
            applyCoachAdvice(analysis, buildFallbackAdvice(analysis));
            analysis.setSource("fallback");
        }

        return analysis;
    }

    CoachAdvice parse(String content) throws Exception {
        JsonNode root = objectMapper.readTree(extractJson(content));
        String coachComment = root.path("coachComment").asText("").trim();
        if (coachComment.isBlank()) {
            throw new IllegalArgumentException("AI response has no coachComment");
        }

        List<String> tips = new ArrayList<>();
        root.path("tips").forEach(tip -> {
            String value = tip.asText("").trim();
            if (!value.isBlank()) {
                tips.add(value);
            }
        });
        if (tips.size() != REQUIRED_TIPS) {
            throw new IllegalArgumentException("AI response must contain exactly " + REQUIRED_TIPS + " tips");
        }

        return new CoachAdvice(coachComment, tips);
    }

    CoachAdvice buildFallbackAdvice(ProgressAnalysisResponse analysis) {
        ProgressStatus status = analysis.getStatus();
        String comment = switch (status) {
            case GOAL_REACHED ->
                    "You reached your weight goal. Keep the habits that got you here: consistent training, protein at each meal, and weekly weigh-ins.";
            case INSUFFICIENT_DATA ->
                    analysis.getMessage() + " Once the trend is reliable, we can refine nutrition and training adjustments.";
            case STABLE ->
                    "Your weight is holding steady. Small changes to calories or training volume will help if you want to move toward your goal.";
            case MOVING_AWAY ->
                    "The trend is currently moving away from your target. Do not overcorrect: review sleep, weekly average weight, and meal consistency first.";
            case PROGRESSING ->
                    analysis.getMessage() + " Stay consistent and review the weekly average instead of day-to-day scale noise.";
        };

        List<String> tips = switch (status) {
            case GOAL_REACHED -> List.of(
                    "Weigh yourself once a week under the same conditions.",
                    "Keep protein high to protect muscle while maintaining.",
                    "Schedule a check-in every 4 weeks to confirm the weight stays stable.");
            case INSUFFICIENT_DATA -> List.of(
                    "Log at least 2 weigh-ins spaced over 7 days or more.",
                    "Measure waist or body fat on the same day as weight when possible.",
                    "Keep the same morning routine so measurements stay comparable.");
            case STABLE -> List.of(
                    "Adjust daily calories by about 150 to 200 kcal in the direction of your goal.",
                    "Add one training session or slightly increase weekly volume.",
                    "Track sleep and steps for one week: recovery often explains plateaus.");
            case MOVING_AWAY -> List.of(
                    "Use a 7-day average weight instead of reacting to one scale reading.",
                    "Return to your planned meals and training days before changing everything.",
                    "If the trend continues for 2 more weeks, reduce calories slightly or increase activity.");
            case PROGRESSING -> List.of(
                    "Keep the current calorie target while the weekly trend stays healthy.",
                    "Strength train 2 to 4 times per week to protect muscle.",
                    "Reassess at the estimated goal date rather than after every weigh-in.");
        };

        return new CoachAdvice(comment, tips);
    }

    private void applyCoachAdvice(ProgressAnalysisResponse analysis, CoachAdvice advice) {
        analysis.setCoachComment(advice.coachComment());
        analysis.setTips(advice.tips());
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("AI response is not JSON");
        }
        return content.substring(start, end + 1);
    }

    record CoachAdvice(String coachComment, List<String> tips) {}
}
