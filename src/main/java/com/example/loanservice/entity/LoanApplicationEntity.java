package com.example.loanservice.entity;

import com.example.loanservice.enums.ApplicationStatus;
import com.example.loanservice.enums.LoanPurpose;
import com.example.loanservice.enums.RejectionReason;
import com.example.loanservice.enums.RiskBand;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loan_applications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID applicationUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private UserEntity user;

    private BigDecimal requestedAmount;
    
    private int requestedTenureMonths;
    
    @Enumerated(EnumType.STRING)
    private LoanPurpose loanPurpose;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    private RiskBand riskBand;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<RejectionReason> rejectionReasons;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private LoanOfferEntity offer;
}
