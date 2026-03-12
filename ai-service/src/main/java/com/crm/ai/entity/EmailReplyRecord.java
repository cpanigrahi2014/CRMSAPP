package com.crm.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_reply_records")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailReplyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "original_from")
    private String originalFrom;

    @Column(name = "original_subject")
    private String originalSubject;

    @Column(name = "original_body", columnDefinition = "TEXT")
    private String originalBody;

    @Column(name = "reply_subject")
    private String replySubject;

    @Column(name = "reply_body", columnDefinition = "TEXT", nullable = false)
    private String replyBody;

    @Column(name = "tone")
    @Builder.Default
    private String tone = "professional";

    @Column(name = "suggestions", columnDefinition = "TEXT")
    private String suggestions; // JSON array

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
