package com.crm.account.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "account_notes", indexes = {
        @Index(name = "idx_acct_notes_account", columnList = "account_id"),
        @Index(name = "idx_acct_notes_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountNote extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
}
