package com.example.sporttracking.progress;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "progress_entries")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProgressEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    Long id;

    @NotNull
    @Positive
    @Column(name = "user_id", nullable = false)
    Long userId;

    @NotNull
    @Column(name = "measurement_date", nullable = false)
    LocalDate measurementDate;

    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax("1000.0")
    @Digits(integer = 4, fraction = 2)
    @Column(precision = 6, scale = 2)
    BigDecimal weight;

    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax("1000.0")
    @Digits(integer = 4, fraction = 2)
    @Column(name = "waist_circumference", precision = 6, scale = 2)
    BigDecimal waistCircumference;

    @DecimalMin("0.0")
    @DecimalMax("24.0")
    @Digits(integer = 2, fraction = 2)
    @Column(name = "sleep_hours", precision = 4, scale = 2)
    BigDecimal sleepHours;

    @Min(1)
    @Max(5)
    @Column(name = "energy_level")
    Integer energyLevel;

    @Builder
    public ProgressEntry(Long userId, LocalDate measurementDate, BigDecimal weight,
                         BigDecimal waistCircumference, BigDecimal sleepHours, Integer energyLevel) {
        this.userId = userId;
        this.measurementDate = measurementDate;
        this.weight = weight;
        this.waistCircumference = waistCircumference;
        this.sleepHours = sleepHours;
        this.energyLevel = energyLevel;
    }

}
