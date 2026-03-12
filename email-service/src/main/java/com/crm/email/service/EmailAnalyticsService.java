package com.crm.email.service;

import com.crm.email.dto.EmailAnalyticsDto;
import com.crm.email.entity.EmailMessage;
import com.crm.email.entity.EmailTrackingEvent;
import com.crm.email.repository.EmailMessageRepository;
import com.crm.email.repository.EmailTrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAnalyticsService {

    private final EmailMessageRepository messageRepo;
    private final EmailTrackingEventRepository trackingRepo;

    public EmailAnalyticsDto getAnalytics(String tenantId) {
        // Totals by status
        long totalSent      = countByStatus(tenantId, EmailMessage.Status.SENT);
        long totalDelivered  = countByStatus(tenantId, EmailMessage.Status.DELIVERED);
        long totalFailed     = countByStatus(tenantId, EmailMessage.Status.FAILED);

        // Totals from tracking events
        long totalOpened  = countTrackingEvents(tenantId, EmailTrackingEvent.EventType.OPENED);
        long totalClicked = countTrackingEvents(tenantId, EmailTrackingEvent.EventType.CLICKED);
        long totalBounced = countTrackingEvents(tenantId, EmailTrackingEvent.EventType.BOUNCED);

        // Rates
        double openRate     = totalSent > 0 ? (double) totalOpened / totalSent * 100 : 0;
        double clickRate    = totalSent > 0 ? (double) totalClicked / totalSent * 100 : 0;
        double bounceRate   = totalSent > 0 ? (double) totalBounced / totalSent * 100 : 0;
        double deliveryRate = totalSent > 0 ? (double) totalDelivered / totalSent * 100 : 0;

        // Time-series (last 30 days)
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        LocalDateTime since = thirtyDaysAgo.atStartOfDay();
        Map<String, Long> sentByDay   = buildDayMap(messageRepo.countSentByDay(tenantId, since));
        Map<String, Long> opensByDay  = buildDayMap(trackingRepo.countEventsByDay(tenantId, EmailTrackingEvent.EventType.OPENED.name(), since));
        Map<String, Long> clicksByDay = buildDayMap(trackingRepo.countEventsByDay(tenantId, EmailTrackingEvent.EventType.CLICKED.name(), since));

        // Per-template breakdown (use SENT events count from messages)
        Map<String, Long> sentByTemplate = new LinkedHashMap<>();
        // (can be expanded to join with templates table)

        return EmailAnalyticsDto.builder()
                .totalSent(totalSent)
                .totalDelivered(totalDelivered)
                .totalOpened(totalOpened)
                .totalClicked(totalClicked)
                .totalBounced(totalBounced)
                .totalFailed(totalFailed)
                .openRate(Math.round(openRate * 100.0) / 100.0)
                .clickRate(Math.round(clickRate * 100.0) / 100.0)
                .bounceRate(Math.round(bounceRate * 100.0) / 100.0)
                .deliveryRate(Math.round(deliveryRate * 100.0) / 100.0)
                .sentByDay(sentByDay)
                .opensByDay(opensByDay)
                .clicksByDay(clicksByDay)
                .sentByTemplate(sentByTemplate)
                .build();
    }

    /* ── Helpers ──────────────────────────────────────────────── */

    private long countByStatus(String tenantId, EmailMessage.Status status) {
        try {
            return messageRepo.countByTenantIdAndStatusAndDeletedFalse(tenantId, status);
        } catch (Exception e) {
            return 0;
        }
    }

    private long countTrackingEvents(String tenantId, EmailTrackingEvent.EventType type) {
        try {
            List<Object[]> counts = trackingRepo.countByEventType(tenantId);
            for (Object[] row : counts) {
                if (row[0] != null && row[0].toString().equals(type.name())) {
                    return ((Number) row[1]).longValue();
                }
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Long> buildDayMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        // Fill last 30 days with zeros first
        for (int i = 29; i >= 0; i--) {
            map.put(LocalDate.now().minusDays(i).toString(), 0L);
        }
        // Overlay actual data
        if (rows != null) {
            for (Object[] row : rows) {
                String day = row[0].toString();
                Long count = ((Number) row[1]).longValue();
                map.put(day, count);
            }
        }
        return map;
    }
}
