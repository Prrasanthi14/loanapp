package com.example.loanservice.controller;

import com.example.loanservice.dto.ApplicationResponse;
import com.example.loanservice.dto.LoanApplicationRequest;
import com.example.loanservice.service.LoanApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
@Slf4j
public class LoanController {

    private final LoanApplicationService service;

    @PostMapping
    public ResponseEntity<ApplicationResponse> evaluateApplication(@Valid @RequestBody LoanApplicationRequest request) {
        log.info("Received API Request to evaluate loan application for: {}", request.getApplicant().getName());
        return ResponseEntity.ok(service.evaluateApplication(request));
    }
}
