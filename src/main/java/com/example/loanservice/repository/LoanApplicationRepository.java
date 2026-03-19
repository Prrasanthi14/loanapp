package com.example.loanservice.repository;

import com.example.loanservice.entity.LoanApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository extends JpaRepository<LoanApplicationEntity, Long> {
}
