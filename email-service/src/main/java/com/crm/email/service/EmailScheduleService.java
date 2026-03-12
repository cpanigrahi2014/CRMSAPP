package com.crm.email.service;

import com.crm.email.dto.EmailScheduleDto;
import com.crm.email.entity.*;
import com.crm.email.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailScheduleService {

    private final EmailScheduleRepository scheduleRepo;
    private final EmailMessageRepository messageRepo;
    private final EmailSendService sendService;

    /* ── CRUD ─────────────────────────────────────────────────── */

    public Page<EmailScheduleDto> list(String tenantId, Pageable pageable) {
        return scheduleRepo.findByTenantIdAndDeletedFalse(tenantId, pageable).map(this::toDto);
    }

    public EmailScheduleDto getById(String tenantId, UUID id) {
        return toDto(find(tenantId, id));
    }

    @Transactional
    public EmailScheduleDto create(String tenantId, String userId,
                                   String to, String cc, String subject,
                                   String bodyHtml, String bodyText,
                                   UUID templateId, LocalDateTime scheduledAt) {
        EmailSchedule s = new EmailSchedule();
        s.setTenantId(tenantId);
        s.setCreatedBy(userId);
        s.setToAddresses(to);
        s.setCcAddresses(cc);
        s.setSubject(subject);
        s.setBodyHtml(bodyHtml);
        s.setBodyText(bodyText);
        s.setTemplateId(templateId);
        s.setScheduledAt(scheduledAt);
        s.setStatus(EmailSchedule.ScheduleStatus.PENDING);
        return toDto(scheduleRepo.save(s));
    }

    @Transactional
    public EmailScheduleDto cancel(String tenantId, UUID id) {
        EmailSchedule s = find(tenantId, id);
        if (s.getStatus() != EmailSchedule.ScheduleStatus.PENDING) {
            throw new RuntimeException("Can only cancel PENDING schedules. Current status: " + s.getStatus());
        }
        s.setStatus(EmailSchedule.ScheduleStatus.CANCELLED);
        return toDto(scheduleRepo.save(s));
    }

    @Transactional
    public void delete(String tenantId, UUID id) {
        EmailSchedule s = find(tenantId, id);
        s.setDeleted(true);
        scheduleRepo.save(s);
    }

    /* ── Scheduled Job: process pending schedules ────────────── */

    @Scheduled(fixedRate = 30000) // every 30 seconds
    @Transactional
    public void processScheduledEmails() {
        LocalDateTime now = LocalDateTime.now();

        // 1) Process EmailSchedule table entries
        List<EmailSchedule> readySchedules = scheduleRepo.findReadyToSend(now);
        for (EmailSchedule schedule : readySchedules) {
            try {
                schedule.setStatus(EmailSchedule.ScheduleStatus.SENT);
                schedule.setSentAt(now);
                scheduleRepo.save(schedule);
                log.info("Processed scheduled email: {}", schedule.getId());
            } catch (Exception e) {
                schedule.setStatus(EmailSchedule.ScheduleStatus.FAILED);
                schedule.setErrorMessage(e.getMessage());
                scheduleRepo.save(schedule);
                log.error("Failed scheduled email {}: {}", schedule.getId(), e.getMessage());
            }
        }

        // 2) Process queued EmailMessage entries (from send with scheduledAt)
        List<EmailMessage> queuedMessages = messageRepo.findScheduledReadyToSend(now);
        for (EmailMessage msg : queuedMessages) {
            sendService.sendExistingMessage(msg);
        }

        if (!readySchedules.isEmpty() || !queuedMessages.isEmpty()) {
            log.info("Processed {} schedules and {} queued messages",
                    readySchedules.size(), queuedMessages.size());
        }
    }

    /* ── Helpers ──────────────────────────────────────────────── */

    private EmailSchedule find(String tenantId, UUID id) {
        return scheduleRepo.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Email schedule not found: " + id));
    }

    private EmailScheduleDto toDto(EmailSchedule s) {
        return EmailScheduleDto.builder()
                .id(s.getId())
                .toAddresses(s.getToAddresses())
                .ccAddresses(s.getCcAddresses())
                .subject(s.getSubject())
                .bodyHtml(s.getBodyHtml())
                .templateId(s.getTemplateId())
                .scheduledAt(s.getScheduledAt())
                .status(s.getStatus().name())
                .sentAt(s.getSentAt())
                .errorMessage(s.getErrorMessage())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
