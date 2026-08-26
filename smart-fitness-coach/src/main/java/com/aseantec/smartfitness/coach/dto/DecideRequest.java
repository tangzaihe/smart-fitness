package com.aseantec.smartfitness.coach.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class DecideRequest {
    @NotBlank
    private String action;
    private String note;
}
