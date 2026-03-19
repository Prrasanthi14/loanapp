package com.example.loanservice.service;

import com.example.loanservice.domain.Applicant;
import com.example.loanservice.domain.ApplicationStatus;
import com.example.loanservice.domain.EmploymentType;
import com.example.loanservice.domain.LoanDetail;
import com.example.loanservice.domain.RiskBand;
import com.example.loanservice.dto.ApplicationResponse;
import com.example.loanservice.dto.LoanApplicationRequest;
import com.example.loanservice.dto.Offer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LoanApplicationService {

    private static final BigDecimal BASE_INTEREST_RATE = new BigDecimal("12.00");
    private static final BigDecimal LARGE_LOAN_THRESHOLD = new BigDecimal("1000000");

    public ApplicationResponse evaluateApplication(LoanApplicationRequest request) {
        Applicant applicant = request.getApplicant();
        LoanDetail loan = request.getLoan();

        List<String> rejectionReasons = new ArrayList<>();

        // Basic Eligibility Rules
        if (applicant.getCreditScore() < 600) {
            rejectionReasons.add("CREDIT_SCORE_TOO_LOW");
        }

        BigDecimal ageAsDecimal = new BigDecimal(applicant.getAge());
        BigDecimal tenureInYears = new BigDecimal(loan.getTenureMonths()).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        if (ageAsDecimal.add(tenureInYears).compareTo(new BigDecimal("65")) > 0) {
            rejectionReasons.add("AGE_TENURE_LIMIT_EXCEEDED");
        }

        RiskBand riskBand = determineRiskBand(applicant.getCreditScore());

        if (!rejectionReasons.isEmpty()) {
            return buildRejectionResponse(rejectionReasons);
        }

        // Calculate Interest Rate
        BigDecimal finalInterestRate = calculateInterestRate(applicant, loan, riskBand);

        // Calculate EMI
        BigDecimal emi = calculateEMI(loan.getAmount(), finalInterestRate, loan.getTenureMonths());

        // Check EMI eligibility
        BigDecimal monthlyIncome = applicant.getMonthlyIncome();
        BigDecimal sixtyPercentIncome = monthlyIncome.multiply(new BigDecimal("0.60"));
        BigDecimal fiftyPercentIncome = monthlyIncome.multiply(new BigDecimal("0.50"));

        if (emi.compareTo(sixtyPercentIncome) > 0) {
            rejectionReasons.add("EMI_EXCEEDS_60_PERCENT");
        }

        if (emi.compareTo(fiftyPercentIncome) > 0) {
            if (!rejectionReasons.contains("EMI_EXCEEDS_60_PERCENT")) {
                 rejectionReasons.add("EMI_EXCEEDS_50_PERCENT");
            }
        }

        if (!rejectionReasons.isEmpty()) {
            return buildRejectionResponse(rejectionReasons);
        }

        BigDecimal totalPayable = emi.multiply(new BigDecimal(loan.getTenureMonths()));

        Offer offer = Offer.builder()
                .interestRate(finalInterestRate)
                .tenureMonths(loan.getTenureMonths())
                .emi(emi.setScale(2, RoundingMode.HALF_UP))
                .totalPayable(totalPayable.setScale(2, RoundingMode.HALF_UP))
                .build();

        return ApplicationResponse.builder()
                .applicationId(UUID.randomUUID())
                .status(ApplicationStatus.APPROVED)
                .riskBand(riskBand)
                .offer(offer)
                .build();
    }

    protected RiskBand determineRiskBand(int creditScore) {
        if (creditScore >= 750) {
            return RiskBand.LOW;
        } else if (creditScore >= 650) {
            return RiskBand.MEDIUM;
        } else {
            return RiskBand.HIGH;
        }
    }

    protected BigDecimal calculateInterestRate(Applicant applicant, LoanDetail loan, RiskBand riskBand) {
        BigDecimal rate = BASE_INTEREST_RATE;

        // Risk Premium
        if (riskBand == RiskBand.MEDIUM) {
            rate = rate.add(new BigDecimal("1.5"));
        } else if (riskBand == RiskBand.HIGH) {
            rate = rate.add(new BigDecimal("3.0"));
        }

        // Employment Premium
        if (applicant.getEmploymentType() == EmploymentType.SELF_EMPLOYED) {
            rate = rate.add(new BigDecimal("1.0"));
        }

        // Loan Size Premium
        if (loan.getAmount().compareTo(LARGE_LOAN_THRESHOLD) > 0) {
            rate = rate.add(new BigDecimal("0.5"));
        }

        return rate;
    }

    protected BigDecimal calculateEMI(BigDecimal principal, BigDecimal annualInterestRate, int tenureMonths) {
        BigDecimal monthlyRate = annualInterestRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusRPowerN = BigDecimal.ONE.add(monthlyRate).pow(tenureMonths);

        BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private ApplicationResponse buildRejectionResponse(List<String> reasons) {
        return ApplicationResponse.builder()
                .applicationId(UUID.randomUUID())
                .status(ApplicationStatus.REJECTED)
                .rejectionReasons(reasons)
                .build();
    }
}
