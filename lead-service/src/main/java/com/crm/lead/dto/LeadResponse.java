package com.crm.lead.dto;

import com.crm.lead.entity.Lead;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String company;
    private String title;
    private Lead.LeadStatus status;
    private Lead.LeadSource source;
    private Integer leadScore;
    private UUID assignedTo;
    private String description;
    private boolean converted;
    private UUID opportunityId;
    private UUID campaignId;
    private String territory;
    private LocalDateTime slaDueDate;
    private LocalDateTime firstResponseAt;
    private UUID accountId;
    private UUID contactId;
    private List<LeadTagResponse> tags;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
