package com.aseantec.smartfitness.training.dto;

import lombok.Data;
import java.math.BigDecimal;
@Data
public class PatchSetRequest {
    private Integer reps;
    private BigDecimal loadKg;
    private BigDecimal rpe;
    private Boolean completed;
    private String exerciseCode;
}
