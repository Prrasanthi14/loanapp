package com.example.loanservice.service;

import com.example.loanservice.domain.Applicant;
import com.example.loanservice.domain.ApplicationStatus;
import com.example.loanservice.domain.EmploymentType;
import com.example.loanservice.domain.LoanDetail;
import com.example.loanservice.domain.RejectionReason;
import com.example.loanservice.domain.RiskBand;
import com.example.loanservice.dto.ApplicationResponse;
import com.example.loanservice.dto.LoanApplicationRequest;
import com.example.loanservice.dto.Offer;
import com.example.loanservice.domain.LoanEvaluationResult;
import com.example.loanservice.repository.LoanEvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanApplicationService {

    private final LoanEvaluationRepository repository;

    private static final BigDecimal BASE_INTEREST_RATE = new BigDecimal("12.00");
    private static final BigDecimal LARGE_LOAN_THRESHOLD = new BigDecimal("1000000");

    public ApplicationResponse evaluateApplication(LoanApplicationRequest request) {
        Applicant applicant = request.getApplicant();
        LoanDetail loan = request.getLoan();

        List<RejectionReason> rejectionReasons = new ArrayList<>();

        // Basic Eligibility Rules
        if (applicant.getCreditScore() < 600) {
            rejectionReasons.add(RejectionReason.CREDIT_SCORE_TOO_LOW);
        }

        BigDecimal ageAsDecimal = new BigDecimal(applicant.getAge());
        BigDecimal tenureInYears = new BigDecimal(loan.getTenureMonths()).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        if (ageAsDecimal.add(tenureInYears).compareTo(new BigDecimal("65")) > 0) {
            rejectionReasons.add(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
        }

        RiskBand riskBand = determineRiskBand(applicant.getCreditScore());

        if (!rejectionReasons.isEmpty()) {
            return buildRejectionResponse(applicant, loan, rejectionReasons);
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
            rejectionReasons.add(RejectionReason.EMI_EXCEEDS_60_PERCENT);
        }

        if (emi.compareTo(fiftyPercentIncome) > 0) {
            if (!rejectionReasons.contains(RejectionReason.EMI_EXCEEDS_60_PERCENT)) {
                 rejectionReasons.add(RejectionReason.EMI_EXCEEDS_50_PERCENT);
            }
        }

        if (!rejectionReasons.isEmpty()) {
            return buildRejectionResponse(applicant, loan, rejectionReasons);
        }

        BigDecimal totalPayable = emi.multiply(new BigDecimal(loan.getTenureMonths()));

        Offer offer = Offer.builder()
                .interestRate(finalInterestRate)
                .tenureMonths(loan.getTenureMonths())
                .emi(emi.setScale(2, RoundingMode.HALF_UP))
                .totalPayable(totalPayable.setScale(2, RoundingMode.HALF_UP))
                .build();

        LoanEvaluationResult entity = LoanEvaluationResult.builder()
                .applicantName(applicant.getName())
                .applicantAge(applicant.getAge())
                .monthlyIncome(applicant.getMonthlyIncome())
                .employmentType(applicant.getEmploymentType())
                .creditScore(applicant.getCreditScore())
                .requestedAmount(loan.getAmount())
                .requestedTenureMonths(loan.getTenureMonths())
                .loanPurpose(loan.getPurpose())
                .status(ApplicationStatus.APPROVED)
                .riskBand(riskBand)
                .offerInterestRate(offer.getInterestRate())
                .offerTenureMonths(offer.getTenureMonths())
                .offerEmi(offer.getEmi())
                .offerTotalPayable(offer.getTotalPayable())
                .build();

        repository.save(entity);

        return ApplicationResponse.builder()
                .applicationId(entity.getId())
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

    private ApplicationResponse buildRejectionResponse(Applicant applicant, LoanDetail loan, List<RejectionReason> reasons) {
        LoanEvaluationResult entity = LoanEvaluationResult.builder()
                .applicantName(applicant.getName())
                .applicantAge(applicant.getAge())
                .monthlyIncome(applicant.getMonthlyIncome())
                .employmentType(applicant.getEmploymentType())
                .creditScore(applicant.getCreditScore())
                .requestedAmount(loan.getAmount())
                .requestedTenureMonths(loan.getTenureMonths())
                .loanPurpose(loan.getPurpose())
                .status(ApplicationStatus.REJECTED)
                .rejectionReasons(reasons)
                .build();

        repository.save(entity);

        return ApplicationResponse.builder()
                .applicationId(entity.getId())
                .status(ApplicationStatus.REJECTED)
                .rejectionReasons(reasons)
                .build();
    }
}
