package com.example.loanservice.repository;

import com.example.loanservice.domain.LoanEvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoanEvaluationRepository extends JpaRepository<LoanEvaluationResult, UUID> {
}
