package com.example.loanservice.service;

import com.example.loanservice.domain.Applicant;
import com.example.loanservice.entity.LoanApplicationEntity;
import com.example.loanservice.entity.LoanOfferEntity;
import com.example.loanservice.enums.ApplicationStatus;
import com.example.loanservice.domain.LoanDetail;
import com.example.loanservice.enums.RejectionReason;
import com.example.loanservice.enums.RiskBand;
import com.example.loanservice.entity.UserEntity;
import com.example.loanservice.dto.ApplicationResponse;
import com.example.loanservice.dto.LoanApplicationRequest;
import com.example.loanservice.dto.Offer;
import com.example.loanservice.repository.LoanApplicationRepository;
import com.example.loanservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanApplicationService {

    private final UserRepository userRepository;
    private final LoanApplicationRepository applicationRepository;
    private final EligibilityRulesEngine rulesEngine;
    private final FinancialCalculatorService calculatorService;

    @Transactional
    public ApplicationResponse evaluateApplication(LoanApplicationRequest request) {
        Applicant applicant = request.getApplicant();
        LoanDetail loan = request.getLoan();

        log.info("Starting evaluation service for Applicant Name: {}", applicant.getName());

        // 1. Fetch or Create the Normalized User Entity
        UserEntity userEntity = userRepository.findByName(applicant.getName())
                .orElseGet(() -> {
                    log.debug("User {} not found. Creating new User record.", applicant.getName());
                    UserEntity newUser = UserEntity.builder()
                            .name(applicant.getName())
                            .age(applicant.getAge())
                            .monthlyIncome(applicant.getMonthlyIncome())
                            .employmentType(applicant.getEmploymentType())
                            .creditScore(applicant.getCreditScore())
                            .build();
                    return userRepository.save(newUser);
                });

        // 2. Base Eligibility Validation
        List<RejectionReason> rejectionReasons = rulesEngine.evaluateBasicRules(applicant, loan);

        // 3. Risk Band Determination (even for rejected applications, we want to log the risk band)
        RiskBand riskBand = calculatorService.determineRiskBand(applicant.getCreditScore());

        if (!rejectionReasons.isEmpty()) {
            log.warn("Basic eligibility failed for user ID {}. Reasons: {}", userEntity.getId(), rejectionReasons);
            return buildRejectionResponse(userEntity, loan, riskBand, rejectionReasons);
        }


        // Calculate the final interest rate based on risk band and other factors
        BigDecimal finalInterestRate = calculatorService.calculateInterestRate(applicant, loan, riskBand);

        // 4. EMI Calculation
        BigDecimal emi = calculatorService.calculateEMI(loan.getAmount(), finalInterestRate, loan.getTenureMonths());

        // Rules checking for EMI affordability
        List<RejectionReason> emiRejections = rulesEngine.evaluateEmiRules(emi, applicant.getMonthlyIncome());
        rejectionReasons.addAll(emiRejections);

        if (!rejectionReasons.isEmpty()) {
            log.warn("Affordability check failed for user ID {}. Reasons: {}", userEntity.getId(), rejectionReasons);
            return buildRejectionResponse(userEntity, loan, riskBand, rejectionReasons);
        }

        // 5. Offer Generation (Approved Path)
        BigDecimal totalPayable = emi.multiply(new BigDecimal(loan.getTenureMonths()));

        Offer offerDto = Offer.builder()
                .interestRate(finalInterestRate)
                .tenureMonths(loan.getTenureMonths())
                .emi(emi.setScale(2, RoundingMode.HALF_UP))
                .totalPayable(totalPayable.setScale(2, RoundingMode.HALF_UP))
                .build();

        return buildApprovedResponse(userEntity, loan, riskBand, offerDto);
    }

    private ApplicationResponse buildApprovedResponse(UserEntity user, LoanDetail loan, RiskBand riskBand, Offer offerDto) {
        
        LoanApplicationEntity appEntity = LoanApplicationEntity.builder()
                .user(user)
                .requestedAmount(loan.getAmount())
                .requestedTenureMonths(loan.getTenureMonths())
                .loanPurpose(loan.getPurpose())
                .status(ApplicationStatus.APPROVED)
                .riskBand(riskBand)
                .build();

        LoanOfferEntity offerEntity = LoanOfferEntity.builder()
                .application(appEntity)
                .interestRate(offerDto.getInterestRate())
                .tenureMonths(offerDto.getTenureMonths())
                .emi(offerDto.getEmi())
                .totalPayable(offerDto.getTotalPayable())
                .build();

        appEntity.setOffer(offerEntity);

        applicationRepository.save(appEntity);
        log.info("APPROVED Loan Application ID: {} generated for User ID: {}", appEntity.getId(), user.getId());

        return ApplicationResponse.builder()
                .applicationId(appEntity.getId())
                .applicationStatus(ApplicationStatus.APPROVED)
                .riskBand(riskBand)
                .offer(offerDto)
                .build();
    }

    private ApplicationResponse buildRejectionResponse(UserEntity user, LoanDetail loan, RiskBand riskBand, List<RejectionReason> reasons) {
        
        LoanApplicationEntity appEntity = LoanApplicationEntity.builder()

                .user(user)
                .requestedAmount(loan.getAmount())
                .requestedTenureMonths(loan.getTenureMonths())
                .loanPurpose(loan.getPurpose())
                .status(ApplicationStatus.REJECTED)
                .riskBand(riskBand)
                .rejectionReasons(reasons)
                .build();

        applicationRepository.save(appEntity);
        log.info("REJECTED Loan Application ID: {} generated for User ID: {}", appEntity.getId(), user.getId());

        return ApplicationResponse.builder()
                .applicationId(appEntity.getId())
                .applicationStatus(ApplicationStatus.REJECTED)
                .rejectionReasons(reasons)
                .build();
    }
}
