package com.example.loanservice.service;

import com.example.loanservice.domain.Applicant;
import com.example.loanservice.domain.LoanDetail;
import com.example.loanservice.domain.RejectionReason;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class EligibilityRulesEngine {

    public List<RejectionReason> evaluateBasicRules(Applicant applicant, LoanDetail loan) {
        List<RejectionReason> reasons = new ArrayList<>();

        if (applicant.getCreditScore() < 600) {
            reasons.add(RejectionReason.CREDIT_SCORE_TOO_LOW);
        }

        BigDecimal ageAsDecimal = new BigDecimal(applicant.getAge());
        BigDecimal tenureInYears = new BigDecimal(loan.getTenureMonths()).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        if (ageAsDecimal.add(tenureInYears).compareTo(new BigDecimal("65")) > 0) {
            reasons.add(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
        }

        return reasons;
    }

    public List<RejectionReason> evaluateEmiRules(BigDecimal emi, BigDecimal monthlyIncome) {
        List<RejectionReason> reasons = new ArrayList<>();

        BigDecimal sixtyPercentIncome = monthlyIncome.multiply(new BigDecimal("0.60"));
        BigDecimal fiftyPercentIncome = monthlyIncome.multiply(new BigDecimal("0.50"));

        if (emi.compareTo(sixtyPercentIncome) > 0) {
            reasons.add(RejectionReason.EMI_EXCEEDS_60_PERCENT);
        }

        if (emi.compareTo(fiftyPercentIncome) > 0) {
            if (!reasons.contains(RejectionReason.EMI_EXCEEDS_60_PERCENT)) {
                reasons.add(RejectionReason.EMI_EXCEEDS_50_PERCENT);
            }
        }

        return reasons;
    }
}
