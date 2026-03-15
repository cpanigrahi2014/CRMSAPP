package com.crm.supportcase.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsatRequest {

    @Min(value = 1, message = "CSAT score must be between 1 and 5")
    @Max(value = 5, message = "CSAT score must be between 1 and 5")
    private int score;

    private String comment;
}
