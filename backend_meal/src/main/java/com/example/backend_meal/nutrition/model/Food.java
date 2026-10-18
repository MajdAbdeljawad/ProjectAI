package com.example.backend_meal.nutrition.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "food")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id", nullable = false)
    @JsonIgnore
    private Meal meal;

    private String name;
    private String quantity;   // e.g. "160g" — stored as String
    private Integer calories;  // ALWAYS numeric, never "40g"
    private Integer protein;
    private Integer carbs;
    private Integer fats;
}
