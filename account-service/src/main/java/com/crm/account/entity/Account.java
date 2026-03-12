package com.crm.account.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_account_tenant", columnList = "tenant_id"),
        @Index(name = "idx_account_name", columnList = "name"),
        @Index(name = "idx_account_parent", columnList = "parent_account_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "industry")
    private String industry;

    @Column(name = "website")
    private String website;

    @Column(name = "phone")
    private String phone;

    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "annual_revenue", precision = 19, scale = 2)
    private BigDecimal annualRevenue;

    @Column(name = "number_of_employees")
    private Integer numberOfEmployees;

    @Column(name = "parent_account_id")
    private UUID parentAccountId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "owner_id")
    private String ownerId;

    @Column(name = "territory", length = 100)
    private String territory;

    @Column(name = "lifecycle_stage", length = 50)
    private String lifecycleStage;

    @Column(name = "health_score")
    private Integer healthScore;

    @Column(name = "segment", length = 100)
    private String segment;

    @Column(name = "engagement_score")
    private Integer engagementScore;
}
