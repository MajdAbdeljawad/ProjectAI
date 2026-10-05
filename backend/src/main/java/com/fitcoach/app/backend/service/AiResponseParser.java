package com.fitcoach.app.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.entity.UserProfile;
import com.fitcoach.app.backend.entity.WorkoutDay;
import com.fitcoach.app.backend.entity.WorkoutExercise;
import com.fitcoach.app.backend.entity.WorkoutPlan;
import com.fitcoach.app.backend.entity.WorkoutWeek;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiResponseParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkoutPlan parse(String jsonResponse, User user, UserProfile profile) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            if (root == null || !root.has("summary") || !root.has("weeks")) {
                throw new IllegalArgumentException("Invalid AI response format");
            }

            WorkoutPlan plan = new WorkoutPlan();
            plan.setUser(user);
            plan.setTitle("30-Day AI Coach Plan");
            plan.setSummary(root.path("summary").asText("30-day adaptive plan"));
            plan.setWeeklyStrategy(root.path("weeklyStrategy").asText("Build consistency and progressive overload"));
            plan.setDurationDays(30);

            JsonNode weeksNode = root.get("weeks");
            List<WorkoutWeek> weeks = new ArrayList<>();
            for (JsonNode weekNode : weeksNode) {
                WorkoutWeek week = new WorkoutWeek();
                week.setWeekNumber(weekNode.path("weekNumber").asInt());
                week.setFocus(weekNode.path("focus").asText("Progressive focus"));
                week.setPlan(plan);

                List<WorkoutDay> days = new ArrayList<>();
                JsonNode daysNode = weekNode.get("days");
                if (daysNode != null) {
                    for (JsonNode dayNode : daysNode) {
                        WorkoutDay day = new WorkoutDay();
                        day.setDayNumber(dayNode.path("day").asInt());
                        day.setType(dayNode.path("type").asText("REST"));
                        day.setNotes(dayNode.path("notes").asText("Keep recovery consistent"));
                        day.setWeek(week);

                        List<WorkoutExercise> exercises = new ArrayList<>();
                        JsonNode exerciseNode = dayNode.get("exercises");
                        if (exerciseNode != null) {
                            for (JsonNode ex : exerciseNode) {
                                WorkoutExercise workoutExercise = new WorkoutExercise();
                                workoutExercise.setName(ex.path("name").asText("Exercise"));
                                workoutExercise.setSets(ex.path("sets").asInt(3));
                                workoutExercise.setReps(ex.path("reps").asInt(10));
                                workoutExercise.setRestSeconds(ex.path("restSeconds").asInt(90));
                                workoutExercise.setNotes(ex.path("notes").asText("Focus on form"));
                                workoutExercise.setDay(day);
                                exercises.add(workoutExercise);
                            }
                        }
                        day.setExercises(exercises);
                        days.add(day);
                    }
                }
                week.setDays(days);
                weeks.add(week);
            }

            plan.setWeeks(weeks);
            return plan;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid AI response JSON", e);
        }
    }
}
