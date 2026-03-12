package com.crm.opportunity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mentions", indexes = {
        @Index(name = "idx_mention_user", columnList = "mentioned_user_id"),
        @Index(name = "idx_mention_record", columnList = "record_type, record_id"),
        @Index(name = "idx_mention_source", columnList = "source_type, source_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mention {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "record_type", nullable = false, length = 50)
    private String recordType;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "mentioned_user_id", nullable = false)
    private String mentionedUserId;

    @Column(name = "mentioned_user_name")
    private String mentionedUserName;

    @Column(name = "mentioned_by_id", nullable = false)
    private String mentionedById;

    @Column(name = "mentioned_by_name")
    private String mentionedByName;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
