package com.crm.email.service;

import com.crm.email.dto.EmailMessageDto;
import com.crm.email.dto.SendEmailRequest;
import com.crm.email.entity.*;
import com.crm.email.repository.*;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSendService {

    private final JavaMailSender mailSender;
    private final EmailMessageRepository messageRepo;
    private final EmailTrackingEventRepository trackingRepo;
    private final EmailAccountService accountService;
    private final EmailTemplateService templateService;

    @Value("${app.email.tracking-base-url:http://localhost:9090}")
    private String trackingBaseUrl;

    @Value("${app.email.from-address:noreply@crm.local}")
    private String defaultFrom;

    @Value("${app.email.from-name:CRM System}")
    private String defaultFromName;

    /* ── Send Email (or queue for scheduling) ─────────────────── */

    @Transactional
    public EmailMessageDto send(String tenantId, String userId, SendEmailRequest req) {
        // 1) Resolve template if provided
        String subject = req.getSubject();
        String bodyHtml = req.getBodyHtml();
        UUID templateId = req.getTemplateId();

        if (templateId != null) {
            var rendered = templateService.render(tenantId, templateId, req.getTemplateVars());
            subject = rendered.subject();
            bodyHtml = rendered.bodyHtml();
        }

        // 2) Create message record
        EmailMessage msg = new EmailMessage();
        msg.setTenantId(tenantId);
        msg.setCreatedBy(userId);
        msg.setFromAddress(defaultFrom);
        msg.setToAddresses(req.getTo());
        msg.setCcAddresses(req.getCc());
        msg.setBccAddresses(req.getBcc());
        msg.setSubject(subject);
        msg.setBodyHtml(bodyHtml);
        msg.setBodyText(req.getBodyText());
        msg.setDirection(EmailMessage.Direction.OUTBOUND);
        msg.setTemplateId(templateId);
        msg.setRelatedEntityType(req.getRelatedEntityType());
        msg.setRelatedEntityId(req.getRelatedEntityId());
        msg.setInReplyTo(req.getInReplyTo());
        msg.setThreadId(req.getInReplyTo() != null ? req.getInReplyTo() : UUID.randomUUID().toString());
        msg.setOpened(false);
        msg.setOpenCount(0);
        msg.setClickCount(0);

        // If accountId is specified, attach it
        if (req.getAccountId() != null) {
            msg.setAccountId(req.getAccountId());
        }

        // 3) Schedule or send immediately
        if (req.getScheduledAt() != null && req.getScheduledAt().isAfter(LocalDateTime.now())) {
            msg.setStatus(EmailMessage.Status.QUEUED);
            msg.setScheduledAt(req.getScheduledAt());
            msg = messageRepo.save(msg);
            log.info("Email queued for scheduled delivery at {} (id={})", req.getScheduledAt(), msg.getId());
            return toDto(msg);
        }

        // 4) Send now
        msg.setStatus(EmailMessage.Status.SENDING);
        msg = messageRepo.save(msg);

        try {
            String finalBody = bodyHtml != null ? bodyHtml : (req.getBodyText() != null ? req.getBodyText() : "");

            // Inject tracking pixel for open tracking
            if (req.isTrackOpens()) {
                finalBody = injectTrackingPixel(finalBody, msg.getId());
            }
            // Rewrite links for click tracking
            if (req.isTrackClicks()) {
                finalBody = rewriteLinksForTracking(finalBody, msg.getId());
            }

            sendViaSMTP(msg.getToAddresses(), msg.getCcAddresses(), msg.getBccAddresses(),
                    msg.getSubject(), finalBody, msg.getFromAddress());

            msg.setStatus(EmailMessage.Status.SENT);
            msg.setSentAt(LocalDateTime.now());

            // Record SENT tracking event
            EmailTrackingEvent sentEvent = new EmailTrackingEvent();
            sentEvent.setMessageId(msg.getId());
            sentEvent.setEventType(EmailTrackingEvent.EventType.SENT);
            trackingRepo.save(sentEvent);

            log.info("Email sent successfully (id={}, to={})", msg.getId(), msg.getToAddresses());
        } catch (Exception e) {
            msg.setStatus(EmailMessage.Status.FAILED);
            msg.setErrorMessage(e.getMessage());
            log.error("Failed to send email (id={}): {}", msg.getId(), e.getMessage(), e);
        }

        return toDto(messageRepo.save(msg));
    }

    /* ── Send already-persisted message (used by scheduler) ──── */

    @Transactional
    public void sendExistingMessage(EmailMessage msg) {
        try {
            msg.setStatus(EmailMessage.Status.SENDING);
            messageRepo.save(msg);

            String finalBody = msg.getBodyHtml() != null ? msg.getBodyHtml() : msg.getBodyText();
            if (finalBody == null) finalBody = "";

            // Always inject tracking on scheduled sends
            finalBody = injectTrackingPixel(finalBody, msg.getId());
            finalBody = rewriteLinksForTracking(finalBody, msg.getId());

            sendViaSMTP(msg.getToAddresses(), msg.getCcAddresses(), msg.getBccAddresses(),
                    msg.getSubject(), finalBody, msg.getFromAddress());

            msg.setStatus(EmailMessage.Status.SENT);
            msg.setSentAt(LocalDateTime.now());

            EmailTrackingEvent sentEvent = new EmailTrackingEvent();
            sentEvent.setMessageId(msg.getId());
            sentEvent.setEventType(EmailTrackingEvent.EventType.SENT);
            trackingRepo.save(sentEvent);

            messageRepo.save(msg);
            log.info("Scheduled email sent (id={})", msg.getId());
        } catch (Exception e) {
            msg.setStatus(EmailMessage.Status.FAILED);
            msg.setErrorMessage(e.getMessage());
            messageRepo.save(msg);
            log.error("Failed to send scheduled email (id={}): {}", msg.getId(), e.getMessage());
        }
    }

    /* ── Query Methods ────────────────────────────────────────── */

    public Page<EmailMessageDto> listMessages(String tenantId, Pageable pageable) {
        return messageRepo.findByTenantIdAndDeletedFalse(tenantId, pageable).map(this::toDto);
    }

    public Page<EmailMessageDto> listByDirection(String tenantId, EmailMessage.Direction dir, Pageable pageable) {
        return messageRepo.findByTenantIdAndDirectionAndDeletedFalse(tenantId, dir, pageable).map(this::toDto);
    }

    public Page<EmailMessageDto> listByStatus(String tenantId, EmailMessage.Status status, Pageable pageable) {
        return messageRepo.findByTenantIdAndStatusAndDeletedFalse(tenantId, status, pageable).map(this::toDto);
    }

    public EmailMessageDto getMessage(String tenantId, UUID id) {
        return messageRepo.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Email message not found"));
    }

    public List<EmailMessageDto> getThread(String tenantId, String threadId) {
        return messageRepo.findByTenantIdAndThreadIdAndDeletedFalseOrderByCreatedAtAsc(tenantId, threadId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<EmailMessageDto> getByEntity(String tenantId, String entityType, UUID entityId) {
        return messageRepo.findByTenantIdAndRelatedEntityTypeAndRelatedEntityIdAndDeletedFalse(
                        tenantId, entityType, entityId, Pageable.unpaged())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public Page<EmailMessageDto> search(String tenantId, String query, Pageable pageable) {
        return messageRepo.search(tenantId, query, pageable)
                .map(this::toDto);
    }

    /* ── SMTP Send ────────────────────────────────────────────── */

    private void sendViaSMTP(String to, String cc, String bcc,
                             String subject, String htmlBody, String from) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(from != null ? from : defaultFrom);
        helper.setTo(to.split("[,;]\\s*"));
        if (cc != null && !cc.isBlank()) helper.setCc(cc.split("[,;]\\s*"));
        if (bcc != null && !bcc.isBlank()) helper.setBcc(bcc.split("[,;]\\s*"));
        helper.setSubject(subject);
        helper.setText(htmlBody, true); // true = isHtml

        mailSender.send(mimeMessage);
    }

    /* ── Tracking Injection ──────────────────────────────────── */

    private String injectTrackingPixel(String html, UUID messageId) {
        String pixel = "<img src=\"" + trackingBaseUrl + "/api/v1/email/track/open/" + messageId +
                ".gif\" width=\"1\" height=\"1\" style=\"display:none\" alt=\"\" />";
        // Insert before closing </body> if present, else append
        if (html.contains("</body>")) {
            return html.replace("</body>", pixel + "</body>");
        }
        return html + pixel;
    }

    private String rewriteLinksForTracking(String html, UUID messageId) {
        // Rewrite <a href="..."> links to go through our click-tracking redirect
        Pattern pattern = Pattern.compile("(<a\\s[^>]*href\\s*=\\s*\")([^\"]+)(\")", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String originalUrl = matcher.group(2);
            // Don't rewrite tracking pixel URLs or mailto: links
            if (originalUrl.contains("/api/v1/email/track/") || originalUrl.startsWith("mailto:")) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                String trackUrl = trackingBaseUrl + "/api/v1/email/track/click/" + messageId +
                        "?url=" + URLEncoder.encode(originalUrl, StandardCharsets.UTF_8);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(1) + trackUrl + matcher.group(3)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /* ── DTO Mapping ─────────────────────────────────────────── */

    private EmailMessageDto toDto(EmailMessage m) {
        return EmailMessageDto.builder()
                .id(m.getId())
                .accountId(m.getAccountId())
                .fromAddress(m.getFromAddress())
                .toAddresses(m.getToAddresses())
                .ccAddresses(m.getCcAddresses())
                .bccAddresses(m.getBccAddresses())
                .subject(m.getSubject())
                .bodyHtml(m.getBodyHtml())
                .bodyText(m.getBodyText())
                .direction(m.getDirection().name())
                .status(m.getStatus().name())
                .threadId(m.getThreadId())
                .inReplyTo(m.getInReplyTo())
                .templateId(m.getTemplateId())
                .relatedEntityType(m.getRelatedEntityType())
                .relatedEntityId(m.getRelatedEntityId())
                .opened(m.isOpened())
                .openCount(m.getOpenCount())
                .clickCount(m.getClickCount())
                .firstOpenedAt(m.getFirstOpenedAt())
                .sentAt(m.getSentAt())
                .scheduledAt(m.getScheduledAt())
                .errorMessage(m.getErrorMessage())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
