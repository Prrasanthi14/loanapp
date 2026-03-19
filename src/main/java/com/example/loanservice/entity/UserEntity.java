package com.example.loanservice.entity;

import com.example.loanservice.enums.EmploymentType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private int age;
    
    private BigDecimal monthlyIncome;
    
    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;
    
    private int creditScore;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<LoanApplicationEntity> applications = new ArrayList<>();
}
