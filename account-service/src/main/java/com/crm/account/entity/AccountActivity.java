package com.crm.account.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "account_activities", indexes = {
        @Index(name = "idx_acct_activity_account", columnList = "account_id"),
        @Index(name = "idx_acct_activity_tenant", columnList = "tenant_id"),
        @Index(name = "idx_acct_activity_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountActivity extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "performed_by")
    private String performedBy;
}
