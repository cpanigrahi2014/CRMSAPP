package com.crm.notification.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.exception.BadRequestException;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.notification.dto.CreateNotificationRequest;
import com.crm.notification.dto.NotificationResponse;
import com.crm.notification.dto.SendSmsRequest;
import com.crm.notification.dto.SendWhatsAppRequest;
import com.crm.notification.entity.Notification;
import com.crm.notification.mapper.NotificationMapper;
import com.crm.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final EventPublisher eventPublisher;
    private final EmailService emailService;
    private final SmsService smsService;
    private final WhatsAppService whatsAppService;

    @Transactional
    public NotificationResponse create(CreateNotificationRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating notification for tenant: {}, recipient: {}", tenantId, request.getRecipient());

        Notification notification = notificationMapper.toEntity(request);
        notification.setTenantId(tenantId);
        notification.setStatus(Notification.NotificationStatus.PENDING);
        notification.setRetryCount(0);

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: {} for tenant: {}", saved.getId(), tenantId);

        eventPublisher.publish("notification-events", tenantId, userId, "Notification",
                saved.getId().toString(), "NOTIFICATION_CREATED", notificationMapper.toResponse(saved));

        return notificationMapper.toResponse(saved);
    }

    @Transactional
    public NotificationResponse send(UUID notificationId, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Sending notification: {} for tenant: {}", notificationId, tenantId);

        Notification notification = notificationRepository.findByIdAndTenantIdAndDeletedFalse(notificationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (notification.getStatus() == Notification.NotificationStatus.SENT) {
            throw new BadRequestException("Notification has already been sent");
        }

        if (notification.getStatus() == Notification.NotificationStatus.CANCELLED) {
            throw new BadRequestException("Notification has been cancelled and cannot be sent");
        }

        try {
            dispatchNotification(notification);
            markAsSent(notification);
            log.info("Notification sent successfully: {}", notificationId);

            eventPublisher.publish("notification-events", tenantId, userId, "Notification",
                    notification.getId().toString(), "NOTIFICATION_SENT", notificationMapper.toResponse(notification));
        } catch (Exception e) {
            log.error("Failed to send notification: {}", notificationId, e);
            markAsFailed(notification, e.getMessage());

            eventPublisher.publish("notification-events", tenantId, userId, "Notification",
                    notification.getId().toString(), "NOTIFICATION_FAILED",
                    java.util.Map.of("reason", e.getMessage()));
        }

        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public NotificationResponse resend(UUID notificationId, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Resending notification: {} for tenant: {}", notificationId, tenantId);

        Notification notification = notificationRepository.findByIdAndTenantIdAndDeletedFalse(notificationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (notification.getStatus() != Notification.NotificationStatus.FAILED) {
            throw new BadRequestException("Only failed notifications can be resent");
        }

        notification.setStatus(Notification.NotificationStatus.PENDING);
        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setFailureReason(null);
        notificationRepository.save(notification);

        return send(notificationId, userId);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(UUID notificationId) {
        String tenantId = TenantContext.getTenantId();
        log.debug("Fetching notification: {} for tenant: {}", notificationId, tenantId);

        Notification notification = notificationRepository.findByIdAndTenantIdAndDeletedFalse(notificationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        return notificationMapper.toResponse(notification);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getAll(int page, int size, String sortBy, String sortDir) {
        String tenantId = TenantContext.getTenantId();
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Notification> notificationPage = notificationRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);

        return buildPagedResponse(notificationPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getByRecipient(String recipient, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        log.debug("Fetching notifications for recipient: {} in tenant: {}", recipient, tenantId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notification> notificationPage = notificationRepository
                .findByTenantIdAndRecipientAndDeletedFalse(tenantId, recipient, pageable);

        return buildPagedResponse(notificationPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getByStatus(Notification.NotificationStatus status, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notification> notificationPage = notificationRepository
                .findByTenantIdAndStatusAndDeletedFalse(tenantId, status, pageable);

        return buildPagedResponse(notificationPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getByType(Notification.NotificationType type, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notification> notificationPage = notificationRepository
                .findByTenantIdAndTypeAndDeletedFalse(tenantId, type, pageable);

        return buildPagedResponse(notificationPage);
    }

    private void dispatchNotification(Notification notification) {
        switch (notification.getType()) {
            case EMAIL -> emailService.sendEmail(
                    notification.getRecipient(),
                    notification.getSubject(),
                    notification.getBody()
            );
            case SMS -> {
                SendSmsRequest smsRequest = SendSmsRequest.builder()
                        .toNumber(notification.getRecipient())
                        .body(notification.getBody())
                        .build();
                smsService.send(smsRequest);
                log.info("SMS dispatched for notification: {}", notification.getId());
            }
            case WHATSAPP -> {
                SendWhatsAppRequest waRequest = SendWhatsAppRequest.builder()
                        .toNumber(notification.getRecipient())
                        .body(notification.getBody())
                        .build();
                whatsAppService.send(waRequest);
                log.info("WhatsApp dispatched for notification: {}", notification.getId());
            }
            case IN_APP -> log.info("In-app notification created: {}", notification.getId());
            case PUSH -> log.warn("Push notification not yet implemented for notification: {}", notification.getId());
        }
    }

    private void markAsSent(Notification notification) {
        notification.setStatus(Notification.NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private void markAsFailed(Notification notification, String reason) {
        notification.setStatus(Notification.NotificationStatus.FAILED);
        notification.setFailureReason(reason);
        notification.setRetryCount(notification.getRetryCount() + 1);
        notificationRepository.save(notification);
    }

    private PagedResponse<NotificationResponse> buildPagedResponse(Page<Notification> page) {
        return PagedResponse.<NotificationResponse>builder()
                .content(page.getContent().stream().map(notificationMapper::toResponse).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
