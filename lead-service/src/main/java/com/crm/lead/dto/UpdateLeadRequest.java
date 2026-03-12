package com.crm.lead.dto;

import com.crm.lead.entity.Lead;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLeadRequest {

    private String firstName;
    private String lastName;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String company;
    private String title;
    private Lead.LeadStatus status;
    private Lead.LeadSource source;
    private Integer leadScore;
    private String description;
    private UUID assignedTo;
    private UUID campaignId;
    private String territory;
    private LocalDateTime slaDueDate;
}
