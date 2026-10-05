package com.fitcoach.app.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workout_exercises")
@Getter
@Setter
@NoArgsConstructor
public class WorkoutExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    @JsonIgnore
    private WorkoutDay day;

    @Column(nullable = false)
    private String name;

    private Integer sets;
    private Integer reps;
    private Integer restSeconds;
    private Integer loadKg;
    private String duration;
    private String intensity;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
