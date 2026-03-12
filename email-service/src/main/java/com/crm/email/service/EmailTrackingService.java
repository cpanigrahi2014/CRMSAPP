package com.crm.email.service;

import com.crm.email.dto.EmailTrackingEventDto;
import com.crm.email.entity.EmailTrackingEvent;
import com.crm.email.repository.EmailMessageRepository;
import com.crm.email.repository.EmailTrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTrackingService {

    private final EmailTrackingEventRepository trackingRepo;
    private final EmailMessageRepository messageRepo;

    /** 1×1 transparent GIF (43 bytes) */
    private static final byte[] TRACKING_PIXEL = {
        0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00,
        0x01, 0x00, (byte)0x80, 0x00, 0x00, (byte)0xff, (byte)0xff, (byte)0xff,
        0x00, 0x00, 0x00, 0x21, (byte)0xf9, 0x04, 0x01, 0x00,
        0x00, 0x00, 0x00, 0x2c, 0x00, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x01, 0x00, 0x00, 0x02, 0x02, 0x44,
        0x01, 0x00, 0x3b
    };

    /* ── Record open event (tracking pixel hit) ───────────────── */

    @Transactional
    public byte[] recordOpen(UUID messageId, String userAgent, String ipAddress) {
        log.debug("Tracking open for message {}", messageId);

        EmailTrackingEvent event = new EmailTrackingEvent();
        event.setMessageId(messageId);
        event.setEventType(EmailTrackingEvent.EventType.OPENED);
        event.setUserAgent(userAgent);
        event.setIpAddress(ipAddress);
        trackingRepo.save(event);

        // Update message counters
        messageRepo.recordOpen(messageId, LocalDateTime.now());

        return TRACKING_PIXEL;
    }

    /* ── Record click event (link redirect) ───────────────────── */

    @Transactional
    public void recordClick(UUID messageId, String url, String userAgent, String ipAddress) {
        log.debug("Tracking click for message {} -> {}", messageId, url);

        EmailTrackingEvent event = new EmailTrackingEvent();
        event.setMessageId(messageId);
        event.setEventType(EmailTrackingEvent.EventType.CLICKED);
        event.setLinkUrl(url);
        event.setUserAgent(userAgent);
        event.setIpAddress(ipAddress);
        trackingRepo.save(event);

        // Update message click counter
        messageRepo.recordClick(messageId);
    }

    /* ── Query events for a message ──────────────────────────── */

    public List<EmailTrackingEventDto> getEventsForMessage(UUID messageId) {
        return trackingRepo.findByMessageIdOrderByCreatedAtDesc(messageId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    /* ── DTO Mapping ─────────────────────────────────────────── */

    private EmailTrackingEventDto toDto(EmailTrackingEvent e) {
        return EmailTrackingEventDto.builder()
                .id(e.getId())
                .messageId(e.getMessageId())
                .eventType(e.getEventType().name())
                .linkUrl(e.getLinkUrl())
                .userAgent(e.getUserAgent())
                .ipAddress(e.getIpAddress())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
