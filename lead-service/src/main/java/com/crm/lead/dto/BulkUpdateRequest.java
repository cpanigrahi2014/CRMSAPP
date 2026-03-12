package com.crm.lead.dto;

import com.crm.lead.entity.Lead;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BulkUpdateRequest {
    @NotEmpty(message = "Lead IDs are required")
    private List<UUID> leadIds;

    private Lead.LeadStatus status;
    private UUID assignTo;
    private String territory;
    private Boolean delete;
}
