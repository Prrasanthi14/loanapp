package com.example.loanservice.service;

import com.example.loanservice.domain.Applicant;
import com.example.loanservice.domain.EmploymentType;
import com.example.loanservice.domain.LoanDetail;
import com.example.loanservice.domain.RiskBand;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FinancialCalculatorService {

    private static final BigDecimal BASE_INTEREST_RATE = new BigDecimal("12.00");
    private static final BigDecimal LARGE_LOAN_THRESHOLD = new BigDecimal("1000000");

    public RiskBand determineRiskBand(int creditScore) {
        if (creditScore >= 750) {
            return RiskBand.LOW;
        } else if (creditScore >= 650) {
            return RiskBand.MEDIUM;
        } else {
            return RiskBand.HIGH;
        }
    }

    public BigDecimal calculateInterestRate(Applicant applicant, LoanDetail loan, RiskBand riskBand) {
        BigDecimal rate = BASE_INTEREST_RATE;

        if (riskBand == RiskBand.MEDIUM) {
            rate = rate.add(new BigDecimal("1.5"));
        } else if (riskBand == RiskBand.HIGH) {
            rate = rate.add(new BigDecimal("3.0"));
        }

        if (applicant.getEmploymentType() == EmploymentType.SELF_EMPLOYED) {
            rate = rate.add(new BigDecimal("1.0"));
        }

        if (loan.getAmount().compareTo(LARGE_LOAN_THRESHOLD) > 0) {
            rate = rate.add(new BigDecimal("0.5"));
        }

        return rate;
    }

    public BigDecimal calculateEMI(BigDecimal principal, BigDecimal annualInterestRate, int tenureMonths) {
        BigDecimal monthlyRate = annualInterestRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusRPowerN = BigDecimal.ONE.add(monthlyRate).pow(tenureMonths);

        BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
