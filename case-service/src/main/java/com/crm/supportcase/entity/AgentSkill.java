package com.crm.supportcase.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "agent_skills", indexes = {
        @Index(name = "idx_as_tenant", columnList = "tenant_id"),
        @Index(name = "idx_as_user", columnList = "user_id"),
        @Index(name = "idx_as_skill", columnList = "skill_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentSkill extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(name = "proficiency", nullable = false)
    @Builder.Default
    private int proficiency = 3;

    @Column(name = "category", length = 50)
    private String category;
}
