package com.example.loanservice.service;

import com.example.loanservice.domain.Applicant;
import com.example.loanservice.domain.ApplicationStatus;
import com.example.loanservice.domain.LoanDetail;
import com.example.loanservice.domain.LoanEvaluationResult;
import com.example.loanservice.domain.RejectionReason;
import com.example.loanservice.domain.RiskBand;
import com.example.loanservice.dto.ApplicationResponse;
import com.example.loanservice.dto.LoanApplicationRequest;
import com.example.loanservice.dto.Offer;
import com.example.loanservice.repository.LoanEvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanApplicationService {

    private final LoanEvaluationRepository repository;
    private final EligibilityRulesEngine rulesEngine;
    private final FinancialCalculatorService calculatorService;

    public ApplicationResponse evaluateApplication(LoanApplicationRequest request) {
        Applicant applicant = request.getApplicant();
        LoanDetail loan = request.getLoan();

        // Base Eligibility Validation
        List<RejectionReason> rejectionReasons = rulesEngine.evaluateBasicRules(applicant, loan);

        // Determining the risk band based on credit score
        RiskBand riskBand = calculatorService.determineRiskBand(applicant.getCreditScore());

        if (!rejectionReasons.isEmpty()) {
            return buildRejectionResponse(applicant, loan, rejectionReasons);
        }


        // Calculate the final interest rate based on risk band and other factors
        BigDecimal finalInterestRate = calculatorService.calculateInterestRate(applicant, loan, riskBand);

        // Calculate EMI based on the final interest rate, loan amount, and tenure
        BigDecimal emi = calculatorService.calculateEMI(loan.getAmount(), finalInterestRate, loan.getTenureMonths());

        // Rules checking for EMI affordability
        List<RejectionReason> emiRejections = rulesEngine.evaluateEmiRules(emi, applicant.getMonthlyIncome());
        rejectionReasons.addAll(emiRejections);

        if (!rejectionReasons.isEmpty()) {
            return buildRejectionResponse(applicant, loan, rejectionReasons);
        }

        // 4. Offer Generation (Orchestration Completion)
        BigDecimal totalPayable = emi.multiply(new BigDecimal(loan.getTenureMonths()));

        Offer offer = Offer.builder()
                .interestRate(finalInterestRate)
                .tenureMonths(loan.getTenureMonths())
                .emi(emi.setScale(2, RoundingMode.HALF_UP))
                .totalPayable(totalPayable.setScale(2, RoundingMode.HALF_UP))
                .build();

        return buildApprovedResponse(applicant, loan, riskBand, offer);
    }

    private ApplicationResponse buildApprovedResponse(Applicant applicant, LoanDetail loan, RiskBand riskBand, Offer offer) {
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
