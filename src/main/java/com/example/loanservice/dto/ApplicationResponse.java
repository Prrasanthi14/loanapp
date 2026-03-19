package com.example.loanservice.dto;

import com.example.loanservice.domain.ApplicationStatus;
import com.example.loanservice.domain.RejectionReason;
import com.example.loanservice.domain.RiskBand;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ApplicationResponse {
    private UUID applicationId;
    private ApplicationStatus status;
    private RiskBand riskBand;
    private Offer offer;
    private List<RejectionReason> rejectionReasons;
}
