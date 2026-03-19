package com.example.loanservice.service;

import com.example.loanservice.domain.Applicant;
import com.example.loanservice.domain.ApplicationStatus;
import com.example.loanservice.domain.EmploymentType;
import com.example.loanservice.domain.LoanDetail;
import com.example.loanservice.domain.LoanPurpose;
import com.example.loanservice.domain.RejectionReason;
import com.example.loanservice.domain.RiskBand;
import com.example.loanservice.dto.ApplicationResponse;
import com.example.loanservice.dto.LoanApplicationRequest;
import com.example.loanservice.repository.LoanEvaluationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    @Mock
    private LoanEvaluationRepository repository;

    @Spy
    private EligibilityRulesEngine rulesEngine = new EligibilityRulesEngine();

    @Spy
    private FinancialCalculatorService calculatorService = new FinancialCalculatorService();

    @InjectMocks
    private LoanApplicationService service;

    @BeforeEach
    void setUp() {
        // Mock save to return the same entity but with an ID
        Mockito.lenient().when(repository.save(Mockito.any())).thenAnswer(invocation -> {
            com.example.loanservice.domain.LoanEvaluationResult entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
    }

    @Test
    void testDetermineRiskBand() {
        assertEquals(RiskBand.LOW, calculatorService.determineRiskBand(750));
        assertEquals(RiskBand.LOW, calculatorService.determineRiskBand(800));
        assertEquals(RiskBand.MEDIUM, calculatorService.determineRiskBand(650));
        assertEquals(RiskBand.MEDIUM, calculatorService.determineRiskBand(700));
        assertEquals(RiskBand.HIGH, calculatorService.determineRiskBand(600));
        assertEquals(RiskBand.HIGH, calculatorService.determineRiskBand(649));
        assertEquals(RiskBand.HIGH, calculatorService.determineRiskBand(300)); // Will be rejected later anyway
    }

    @Test
    void testCalculateEMI() {
        // P = 500000, r = 12% annual = 1% monthly, n = 36 months
        // EMI = 500000 * 0.01 * (1.01)^36 / ((1.01)^36 - 1)
        // 1.01^36 approx 1.430768
        // Approx EMI: 16607.15 (based on pure calculation)

        BigDecimal principal = new BigDecimal("500000");
        BigDecimal rate = new BigDecimal("12.00");
        int tenure = 36;
        
        BigDecimal emi = calculatorService.calculateEMI(principal, rate, tenure);

        // Let's assert against a known good value.
        // 16607.15 is the standard result.
        assertEquals(new BigDecimal("16607.15"), emi);
    }

    @Test
    void testCalculateInterestRate() {
        Applicant applicant = new Applicant();
        applicant.setEmploymentType(EmploymentType.SALARIED);

        LoanDetail loan = new LoanDetail();
        loan.setAmount(new BigDecimal("500000")); // <= 1,000,000

        // Base 12% + LOW 0% + SALARIED 0% + SIZE <= 10L 0%
        assertEquals(new BigDecimal("12.00"), calculatorService.calculateInterestRate(applicant, loan, RiskBand.LOW));

        // Base 12% + MEDIUM 1.5% + SELF_EMPLOYED 1.0% + SIZE > 10L 0.5%
        applicant.setEmploymentType(EmploymentType.SELF_EMPLOYED);
        loan.setAmount(new BigDecimal("1500000"));
        BigDecimal rate = calculatorService.calculateInterestRate(applicant, loan, RiskBand.MEDIUM);
        assertEquals(new BigDecimal("15.00"), rate);
    }

    @Test
    void testEligibility_RejectWhenCreditScoreTooLow() {
        LoanApplicationRequest request = createValidRequest();
        request.getApplicant().setCreditScore(599);

        ApplicationResponse response = service.evaluateApplication(request);

        assertEquals(ApplicationStatus.REJECTED, response.getStatus());
        assertTrue(response.getRejectionReasons().contains(RejectionReason.CREDIT_SCORE_TOO_LOW));
    }

    @Test
    void testEligibility_RejectWhenAgeAndTenureExceedLimit() {
        LoanApplicationRequest request = createValidRequest();
        request.getApplicant().setAge(60);
        request.getLoan().setTenureMonths(72); // 60 + 6 = 66 > 65

        ApplicationResponse response = service.evaluateApplication(request);

        assertEquals(ApplicationStatus.REJECTED, response.getStatus());
        assertTrue(response.getRejectionReasons().contains(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED));
    }

    @Test
    void testEligibility_RejectWhenEmiExceeds60PercentOfIncome() {
        LoanApplicationRequest request = createValidRequest();
        // Income = 10k, EMI = 16k+
        request.getApplicant().setMonthlyIncome(new BigDecimal("10000"));

        ApplicationResponse response = service.evaluateApplication(request);

        assertEquals(ApplicationStatus.REJECTED, response.getStatus());
        assertTrue(response.getRejectionReasons().contains(RejectionReason.EMI_EXCEEDS_60_PERCENT));
    }

    @Test
    void testEligibility_RejectWhenEmiExceeds50PercentOfIncome() {
        LoanApplicationRequest request = createValidRequest();
        // Base EMI is 16607.15
        // Let's set income to 30000. 50% = 15000, 60% = 18000.
        // So EMI > 50% but not > 60%.
        request.getApplicant().setMonthlyIncome(new BigDecimal("30000"));

        ApplicationResponse response = service.evaluateApplication(request);

        assertEquals(ApplicationStatus.REJECTED, response.getStatus());
        assertTrue(response.getRejectionReasons().contains(RejectionReason.EMI_EXCEEDS_50_PERCENT));
    }

    @Test
    void testApproveRequest() {
        LoanApplicationRequest request = createValidRequest();

        ApplicationResponse response = service.evaluateApplication(request);

        assertEquals(ApplicationStatus.APPROVED, response.getStatus());
        assertNotNull(response.getOffer());
        assertEquals(new BigDecimal("16607.15"), response.getOffer().getEmi());
        assertEquals(new BigDecimal("597857.40"), response.getOffer().getTotalPayable());
    }

    private LoanApplicationRequest createValidRequest() {
        Applicant applicant = new Applicant();
        applicant.setName("John Doe");
        applicant.setAge(30);
        applicant.setMonthlyIncome(new BigDecimal("75000"));
        applicant.setEmploymentType(EmploymentType.SALARIED);
        applicant.setCreditScore(720); // MEDIUM band -> +1.5%

        LoanDetail loan = new LoanDetail();
        // Base EMI example P=500k, n=36, rate = 12% + 1.5% = 13.5%
        // Actually, previous EMI test was for 12%. Let's set credit score to 750 (LOW
        // band) so rate is 12%.
        applicant.setCreditScore(750);
        loan.setAmount(new BigDecimal("500000"));
        loan.setTenureMonths(36);
        loan.setPurpose(LoanPurpose.PERSONAL);

        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicant(applicant);
        request.setLoan(loan);

        return request;
    }
}
