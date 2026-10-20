package com.example.sporttracking.goal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;

@Entity
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Positive
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 32)
    private GoalType type;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 8, fraction = 2, message = "La valeur cible accepte au maximum 8 chiffres entiers et 2 décimales")
    @Column(name = "target_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal targetValue;

    @NotBlank
    @Size(max = 24)
    @Column(nullable = false, length = 24)
    private String unit;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GoalStatus status = GoalStatus.ACTIVE;

    protected Goal() {
    }

    @Builder
    public Goal(Long userId, GoalType type, BigDecimal targetValue, String unit,
                LocalDate startDate, LocalDate targetDate, GoalStatus status) {
        this.userId = userId;
        this.type = type;
        this.targetValue = targetValue;
        this.unit = unit;
        this.startDate = startDate;
        this.targetDate = targetDate;
        this.status = status == null ? GoalStatus.ACTIVE : status;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public GoalType getType() { return type; }
    public BigDecimal getTargetValue() { return targetValue; }
    public String getUnit() { return unit; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getTargetDate() { return targetDate; }
    public GoalStatus getStatus() { return status; }

    @AssertTrue(message = "La date cible doit être égale ou postérieure à la date de début")
    public boolean isTargetDateOnOrAfterStartDate() {
        return startDate == null || targetDate == null || !targetDate.isBefore(startDate);
    }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setType(GoalType type) { this.type = type; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public void setStatus(GoalStatus status) { this.status = status; }
}
