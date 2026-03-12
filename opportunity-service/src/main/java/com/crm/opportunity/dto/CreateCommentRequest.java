package com.crm.opportunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotBlank
    private String recordType;

    @NotNull
    private UUID recordId;

    @NotBlank
    private String content;

    private UUID parentCommentId;

    @Builder.Default
    private Boolean isInternal = true;
}
