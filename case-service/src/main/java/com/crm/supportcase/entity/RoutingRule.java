package com.crm.supportcase.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "routing_rules", indexes = {
        @Index(name = "idx_rr_tenant", columnList = "tenant_id"),
        @Index(name = "idx_rr_queue", columnList = "queue_id"),
        @Index(name = "idx_rr_priority", columnList = "rule_priority")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingRule extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "queue_id", nullable = false)
    private UUID queueId;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_field", nullable = false, length = 30)
    private MatchField matchField;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_operator", nullable = false, length = 20)
    @Builder.Default
    private MatchOperator matchOperator = MatchOperator.EQUALS;

    @Column(name = "match_value", nullable = false)
    private String matchValue;

    @Column(name = "required_skill", length = 100)
    private String requiredSkill;

    @Column(name = "min_proficiency")
    @Builder.Default
    private int minProficiency = 1;

    @Column(name = "rule_priority", nullable = false)
    @Builder.Default
    private int rulePriority = 10;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    public enum MatchField {
        PRIORITY, ORIGIN, SUBJECT, ACCOUNT_NAME, CONTACT_EMAIL, STATUS
    }

    public enum MatchOperator {
        EQUALS, NOT_EQUALS, CONTAINS, STARTS_WITH, REGEX
    }
}
