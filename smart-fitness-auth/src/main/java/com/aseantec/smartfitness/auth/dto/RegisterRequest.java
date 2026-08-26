package com.aseantec.smartfitness.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    @Email
    private String email;
    private String phone;
    @NotBlank
    @Size(min = 8, max = 64)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "password must contain letter and digit")
    private String password;
}
