package com.example.loanservice.domain;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanDetail {
    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "10000.00", message = "Loan amount must be between 10,000 and 50,00,000")
    @DecimalMax(value = "5000000.00", message = "Loan amount must be between 10,000 and 50,00,000")
    private BigDecimal amount;

    @Min(value = 6, message = "Tenure must be between 6 and 360 months")
    @Max(value = 360, message = "Tenure must be between 6 and 360 months")
    private int tenureMonths;

    @NotNull(message = "Loan purpose is required")
    private LoanPurpose purpose;
}
