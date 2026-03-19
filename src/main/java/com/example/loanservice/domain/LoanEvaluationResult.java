package com.example.loanservice.domain;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loan_evaluations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanEvaluationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Applicant data
    private String applicantName;
    private int applicantAge;
    private BigDecimal monthlyIncome;
    
    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;
    
    private int creditScore;

    // Loan request data
    private BigDecimal requestedAmount;
    private int requestedTenureMonths;
    
    @Enumerated(EnumType.STRING)
    private LoanPurpose loanPurpose;

    // Evaluation results
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;
    
    @Enumerated(EnumType.STRING)
    private RiskBand riskBand;
    
    // Offer data (nullable for REJECTED)
    private BigDecimal offerInterestRate;
    private Integer offerTenureMonths;
    private BigDecimal offerEmi;
    private BigDecimal offerTotalPayable;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<RejectionReason> rejectionReasons;
}
