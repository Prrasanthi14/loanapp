package com.example.loanservice.controller;

import com.example.loanservice.dto.ApplicationResponse;
import com.example.loanservice.dto.LoanApplicationRequest;
import com.example.loanservice.service.LoanApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class LoanController {

    private final LoanApplicationService loanApplicationService;

    @PostMapping
    public ResponseEntity<ApplicationResponse> evaluateApplication(@Valid @RequestBody LoanApplicationRequest request) {
        
        ApplicationResponse response = loanApplicationService.evaluateApplication(request);
        return ResponseEntity.ok(response);
    }
}
