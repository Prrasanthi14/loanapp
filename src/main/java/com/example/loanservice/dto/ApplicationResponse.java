package com.example.loanservice.dto;

import com.example.loanservice.enums.ApplicationStatus;
import com.example.loanservice.enums.RejectionReason;
import com.example.loanservice.enums.RiskBand;
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
