package com.crm.email.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "email_templates")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EmailTemplate extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "body_html", nullable = false, columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "variables", columnDefinition = "TEXT")
    private String variables;  // JSON array: ["firstName","companyName", ...]

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private int usageCount = 0;
}
