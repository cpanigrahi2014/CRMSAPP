package com.crm.campaign.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AddMembersRequest {
    @NotEmpty(message = "Lead IDs list cannot be empty")
    private List<UUID> leadIds;
}
