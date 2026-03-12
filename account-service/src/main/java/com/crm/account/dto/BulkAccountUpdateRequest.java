package com.crm.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAccountUpdateRequest {
    private List<String> accountIds;
    private String ownerId;
    private String territory;
    private String type;
    private String lifecycleStage;
    private String segment;
}
