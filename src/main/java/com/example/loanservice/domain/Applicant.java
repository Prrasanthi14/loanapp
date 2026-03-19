package com.example.loanservice.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Applicant {
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @Min(value = 21, message = "Age must be at least 21")
    @Max(value = 60, message = "Age must be at most 60")
    private int age;

    @NotNull(message = "Monthly income is required")
    @Positive(message = "Monthly income must be greater than 0")
    private BigDecimal monthlyIncome;

    @NotNull(message = "Employment type is required")
    private EmploymentType employmentType;

    @Min(value = 300, message = "Credit score must be at least 300")
    @Max(value = 900, message = "Credit score must be at most 900")
    private int creditScore;
}
