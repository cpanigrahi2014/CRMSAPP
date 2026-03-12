package com.crm.opportunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    private String productName;

    private String productCode;

    @PositiveOrZero
    @Builder.Default
    private Integer quantity = 1;

    @PositiveOrZero(message = "Unit price must be zero or positive")
    private BigDecimal unitPrice;

    @PositiveOrZero
    private BigDecimal discount;

    private String description;
}
