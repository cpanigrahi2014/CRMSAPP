package com.crm.activity.service;

import com.crm.activity.entity.Activity;
import com.crm.activity.entity.CalendarFeedToken;
import com.crm.activity.repository.ActivityRepository;
import com.crm.activity.repository.CalendarFeedTokenRepository;
import com.crm.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarFeedService {

    private final ActivityRepository activityRepository;
    private final CalendarFeedTokenRepository tokenRepository;

    private static final DateTimeFormatter ICAL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ─── Token Management ────────────────────────────────────────────

    @Transactional
    public CalendarFeedToken generateToken(String tenantId, String userId, String name) {
        CalendarFeedToken token = CalendarFeedToken.builder()
                .tenantId(tenantId)
                .userId(userId)
                .token(generateSecureToken())
                .name(name != null ? name : "Calendar Feed")
                .active(true)
                .build();
        return tokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public List<CalendarFeedToken> getTokens(String tenantId, String userId) {
        return tokenRepository.findByTenantIdAndUserIdAndActiveTrue(tenantId, userId);
    }

    @Transactional
    public void revokeToken(UUID tokenId, String tenantId, String userId) {
        tokenRepository.findByIdAndTenantIdAndUserId(tokenId, tenantId, userId)
                .ifPresent(t -> {
                    t.setActive(false);
                    tokenRepository.save(t);
                });
    }

    @Transactional
    public Optional<CalendarFeedToken> validateToken(String token) {
        Optional<CalendarFeedToken> feedToken = tokenRepository.findByTokenAndActiveTrue(token);
        feedToken.ifPresent(t -> {
            t.setLastAccessedAt(LocalDateTime.now());
            tokenRepository.save(t);
        });
        return feedToken;
    }

    // ─── iCal Feed Generation ────────────────────────────────────────

    @Transactional(readOnly = true)
    public String generateICalFeed(String tenantId) {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        List<Activity> activities = activityRepository.findForCalendarFeed(tenantId, from);
        return buildICalendar(activities, tenantId);
    }

    @Transactional(readOnly = true)
    public String generateSingleEventIcs(String tenantId, UUID activityId) {
        return activityRepository.findByIdAndTenantIdAndDeletedFalse(activityId, tenantId)
                .map(a -> buildICalendar(List.of(a), tenantId))
                .orElse(null);
    }

    // ─── iCalendar RFC 5545 Builder ──────────────────────────────────

    private String buildICalendar(List<Activity> activities, String tenantId) {
        StringBuilder ical = new StringBuilder();
        ical.append("BEGIN:VCALENDAR\r\n");
        ical.append("VERSION:2.0\r\n");
        ical.append("PRODID:-//CRM Platform//Activity Calendar//EN\r\n");
        ical.append("CALSCALE:GREGORIAN\r\n");
        ical.append("METHOD:PUBLISH\r\n");
        ical.append("X-WR-CALNAME:CRM Activities\r\n");
        ical.append("X-WR-TIMEZONE:UTC\r\n");

        for (Activity activity : activities) {
            ical.append(buildVEvent(activity, tenantId));
        }

        ical.append("END:VCALENDAR\r\n");
        return ical.toString();
    }

    private String buildVEvent(Activity activity, String tenantId) {
        StringBuilder event = new StringBuilder();
        event.append("BEGIN:VEVENT\r\n");

        // Unique identifier
        event.append("UID:").append(activity.getId()).append("@crm-").append(tenantId).append("\r\n");

        // Timestamps
        LocalDateTime dtStart = activity.getStartTime() != null ? activity.getStartTime() : activity.getDueDate();
        LocalDateTime dtEnd = activity.getEndTime() != null ? activity.getEndTime() :
                (dtStart != null ? dtStart.plusHours(1) : null);

        if (dtStart != null) {
            event.append("DTSTART:").append(dtStart.format(ICAL_DATE_FORMAT)).append("\r\n");
        }
        if (dtEnd != null) {
            event.append("DTEND:").append(dtEnd.format(ICAL_DATE_FORMAT)).append("\r\n");
        }

        // Created / Modified
        if (activity.getCreatedAt() != null) {
            event.append("CREATED:").append(activity.getCreatedAt().format(ICAL_DATE_FORMAT)).append("\r\n");
        }
        if (activity.getUpdatedAt() != null) {
            event.append("LAST-MODIFIED:").append(activity.getUpdatedAt().format(ICAL_DATE_FORMAT)).append("\r\n");
        }
        event.append("DTSTAMP:").append(LocalDateTime.now().format(ICAL_DATE_FORMAT)).append("\r\n");

        // Summary + Description
        event.append("SUMMARY:").append(escapeIcalText(buildSummary(activity))).append("\r\n");

        if (activity.getDescription() != null && !activity.getDescription().isBlank()) {
            event.append("DESCRIPTION:").append(escapeIcalText(activity.getDescription())).append("\r\n");
        }

        // Location for meetings
        if (activity.getLocation() != null && !activity.getLocation().isBlank()) {
            event.append("LOCATION:").append(escapeIcalText(activity.getLocation())).append("\r\n");
        }

        // Status mapping
        event.append("STATUS:").append(mapStatus(activity.getStatus())).append("\r\n");

        // Priority: iCal uses 1-9 (1=highest)
        event.append("PRIORITY:").append(mapPriority(activity.getPriority())).append("\r\n");

        // Categories
        event.append("CATEGORIES:").append(activity.getType().name()).append("\r\n");

        // Alarm/Reminder
        if (activity.getReminderAt() != null && dtStart != null && !activity.isReminderSent()) {
            long minutesBefore = java.time.Duration.between(activity.getReminderAt(), dtStart).toMinutes();
            if (minutesBefore < 0) minutesBefore = 0; // reminder is after start, trigger at start
            event.append("BEGIN:VALARM\r\n");
            event.append("TRIGGER:-PT").append(Math.abs(minutesBefore)).append("M\r\n");
            event.append("ACTION:DISPLAY\r\n");
            event.append("DESCRIPTION:Reminder: ").append(escapeIcalText(activity.getSubject())).append("\r\n");
            event.append("END:VALARM\r\n");
        }

        event.append("END:VEVENT\r\n");
        return event.toString();
    }

    private String buildSummary(Activity activity) {
        String prefix = switch (activity.getType()) {
            case TASK -> "[Task]";
            case CALL -> "[Call]";
            case MEETING -> "[Meeting]";
            case EMAIL -> "[Email]";
        };
        return prefix + " " + activity.getSubject();
    }

    private String mapStatus(Activity.ActivityStatus status) {
        return switch (status) {
            case NOT_STARTED -> "TENTATIVE";
            case IN_PROGRESS -> "CONFIRMED";
            case COMPLETED -> "CONFIRMED";
            case CANCELLED -> "CANCELLED";
        };
    }

    private int mapPriority(Activity.ActivityPriority priority) {
        if (priority == null) return 5;
        return switch (priority) {
            case URGENT -> 1;
            case HIGH -> 3;
            case MEDIUM -> 5;
            case LOW -> 9;
        };
    }

    private String escapeIcalText(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace(";", "\\;")
                   .replace(",", "\\,")
                   .replace("\n", "\\n")
                   .replace("\r", "");
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
