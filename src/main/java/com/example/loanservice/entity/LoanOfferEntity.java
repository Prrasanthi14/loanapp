package com.example.loanservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "loan_offers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanOfferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    @ToString.Exclude
    private LoanApplicationEntity application;

    private BigDecimal interestRate;
    
    private int tenureMonths;
    
    private BigDecimal emi;
    
    private BigDecimal totalPayable;
}
