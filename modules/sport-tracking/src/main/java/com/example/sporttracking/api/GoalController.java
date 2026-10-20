package com.example.sporttracking.api;

import com.example.sporttracking.api.dto.GoalRequest;
import com.example.sporttracking.api.dto.GoalResponse;
import com.example.sporttracking.goal.Goal;
import com.example.sporttracking.goal.GoalRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/goals")
@Validated
public class GoalController {
    private final GoalRepository goalRepository;

    public GoalController(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    @GetMapping
    public List<GoalResponse> list(@PathVariable @Positive Long userId) {
        return goalRepository.findByUserId(userId).stream().map(GoalResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@PathVariable @Positive Long userId,
                                               @Valid @RequestBody GoalRequest request,
                                               UriComponentsBuilder uriBuilder) {
        Goal goal = Goal.builder()
                .userId(userId)
                .type(request.type())
                .targetValue(request.targetValue())
                .unit(request.unit())
                .startDate(request.startDate())
                .targetDate(request.targetDate())
                .status(request.status())
                .build();
        GoalResponse response = GoalResponse.from(goalRepository.save(goal));
        var location = uriBuilder.path("/api/users/{userId}/goals/{goalId}")
                .buildAndExpand(userId, response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{goalId}")
    public GoalResponse get(@PathVariable @Positive Long userId, @PathVariable @Positive Long goalId) {
        return GoalResponse.from(findGoal(userId, goalId));
    }

    @PutMapping("/{goalId}")
    public GoalResponse update(@PathVariable @Positive Long userId, @PathVariable @Positive Long goalId,
                               @Valid @RequestBody GoalRequest request) {
        Goal goal = findGoal(userId, goalId);
        goal.setType(request.type());
        goal.setTargetValue(request.targetValue());
        goal.setUnit(request.unit());
        goal.setStartDate(request.startDate());
        goal.setTargetDate(request.targetDate());
        goal.setStatus(request.status());
        return GoalResponse.from(goalRepository.save(goal));
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long userId,
                                       @PathVariable @Positive Long goalId) {
        goalRepository.delete(findGoal(userId, goalId));
        return ResponseEntity.noContent().build();
    }

    private Goal findGoal(Long userId, Long goalId) {
        return goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Objectif"));
    }
}
