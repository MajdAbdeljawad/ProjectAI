package com.example.backend_meal.nutrition.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_plan_id", nullable = false)
    @JsonIgnore
    private DailyPlan dailyPlan;

    private String type;           // breakfast, lunch, dinner, morning_snack...
    private String name;
    private Integer calories;      // recalculated from foods
    private Integer protein;
    private Integer carbs;
    private Integer fats;
    private String preparationTime;

    @Column(columnDefinition = "TEXT")
    private String preparation;

    private Integer orderIndex;

    @Builder.Default
    private Boolean isCompleted = false;

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Food> foods = new ArrayList<>();

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MealAlternative> alternatives = new ArrayList<>();
}
