package com.example.loanservice.dto;

import com.example.loanservice.enums.ApplicationStatus;
import com.example.loanservice.enums.RejectionReason;
import com.example.loanservice.enums.RiskBand;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ApplicationResponse {
    private Long applicationId;
    private ApplicationStatus applicationStatus;
    private RiskBand riskBand;
    private Offer offer;
    private List<RejectionReason> rejectionReasons;
}
