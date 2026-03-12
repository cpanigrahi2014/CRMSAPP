package com.crm.opportunity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "opportunity_products", indexes = {
        @Index(name = "idx_opp_prod_opportunity", columnList = "opportunity_id"),
        @Index(name = "idx_opp_prod_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpportunityProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_code", length = 100)
    private String productCode;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "discount", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "total_price", precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateTotal();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateTotal();
    }

    public void calculateTotal() {
        if (unitPrice != null && quantity != null) {
            BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
            if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discountAmount = gross.multiply(discount).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                totalPrice = gross.subtract(discountAmount);
            } else {
                totalPrice = gross;
            }
        }
    }
}
