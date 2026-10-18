package com.example.backend_meal.nutrition.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "nutrition_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NutritionProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String userId;
    private String fullName;
    private Integer age;
    private String sex;
    private Double height;
    private Double weight;
    private String goal;
    private String sportsLevel;
    private Integer sessionsPerWeek;
    private String workoutType;
    private String diet;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_allergies", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "allergen")
    @Builder.Default
    private List<String> allergies = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_intolerances", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "intolerance")
    @Builder.Default
    private List<String> intolerances = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_preferred_foods", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "food")
    @Builder.Default
    private List<String> preferredFoods = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_disliked_foods", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "food")
    @Builder.Default
    private List<String> dislikedFoods = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_dietary_restrictions", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "restriction")
    @Builder.Default
    private List<String> dietaryRestrictions = new ArrayList<>();

    private Integer mealsPerDay;
    private String wakeUpTime;
    private String sleepTime;
    private String budget;
    private String cookingTime;

    // Calculated by NutritionCalculatorService
    private Integer bmr;
    private Integer tdee;
    private Integer targetCalories;
    private Integer targetProtein;
    private Integer targetCarbs;
    private Integer targetFats;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "nutritionProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MealPlan> mealPlans = new ArrayList<>();
}
