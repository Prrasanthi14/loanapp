package com.example.loanservice.dto;

import com.example.loanservice.domain.Applicant;
import com.example.loanservice.domain.LoanDetail;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoanApplicationRequest {
    @Valid
    @NotNull(message = "Applicant details are required")
    private Applicant applicant;

    @Valid
    @NotNull(message = "Loan details are required")
    private LoanDetail loan;
}
