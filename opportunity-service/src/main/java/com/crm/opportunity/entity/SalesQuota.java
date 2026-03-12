package com.crm.opportunity.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sales_quotas", indexes = {
        @Index(name = "idx_sales_quota_user", columnList = "user_id"),
        @Index(name = "idx_sales_quota_tenant", columnList = "tenant_id"),
        @Index(name = "idx_sales_quota_period", columnList = "period_start, period_end")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesQuota extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private PeriodType periodType;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "target_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal targetAmount = BigDecimal.ZERO;

    @Column(name = "target_deals")
    @Builder.Default
    private Integer targetDeals = 0;

    @Column(name = "actual_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal actualAmount = BigDecimal.ZERO;

    @Column(name = "actual_deals")
    @Builder.Default
    private Integer actualDeals = 0;

    @Column(name = "attainment_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal attainmentPct = BigDecimal.ZERO;

    public enum PeriodType {
        MONTHLY, QUARTERLY, ANNUAL
    }
}
