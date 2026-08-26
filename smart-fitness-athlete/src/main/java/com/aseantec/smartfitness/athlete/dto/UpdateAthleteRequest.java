package com.aseantec.smartfitness.athlete.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateAthleteRequest {
    @NotBlank
    private String displayName;
    private String sex;
    private LocalDate birthDate;
    private BigDecimal heightCm;
    @NotBlank
    private String goal;
    private Integer weeklyMin;
    @NotEmpty
    private List<String> equipment = new ArrayList<>();
    private Preferences preferences = new Preferences();
    @Valid
    private List<ConstraintItem> constraints = new ArrayList<>();

    @Data
    public static class Preferences {
        private List<String> liked = new ArrayList<>();
        private List<String> disliked = new ArrayList<>();
        private List<String> never = new ArrayList<>();
    }

    @Data
    public static class ConstraintItem {
        @NotBlank
        private String type;
        private String bodyPart;
        private Integer severity;
        private LocalDate startsOn;
        private LocalDate endsOn;
        private String notes;
    }
}
