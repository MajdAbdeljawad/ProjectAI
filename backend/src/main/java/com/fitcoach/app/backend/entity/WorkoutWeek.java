package com.fitcoach.app.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workout_weeks")
@Getter
@Setter
@NoArgsConstructor
public class WorkoutWeek {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnore
    private WorkoutPlan plan;

    @Column(nullable = false)
    private int weekNumber;

    @Column(columnDefinition = "TEXT")
    private String focus;

    @OneToMany(mappedBy = "week", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutDay> days = new ArrayList<>();
}
