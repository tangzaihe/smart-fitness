package com.aseantec.smartfitness.athlete.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WellnessRequest {
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("24.0")
    private BigDecimal sleepHours;
    @NotNull
    @Min(1)
    @Max(10)
    private Integer subjectiveFatigue;
}
