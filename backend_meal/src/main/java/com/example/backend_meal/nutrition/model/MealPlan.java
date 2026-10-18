package com.example.backend_meal.nutrition.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meal_plan")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nutrition_profile_id", nullable = false)
    @JsonIgnore
    private NutritionProfile nutritionProfile;

    private String title;
    private Integer durationDays;
    private Integer caloriesTarget;
    private Integer proteinTarget;
    private Integer carbsTarget;
    private Integer fatsTarget;
    private Boolean generatedByAI;
    private String providerName;
    private String model;
    private Boolean isActive;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    @Builder.Default
    private List<DailyPlan> days = new ArrayList<>();
}
