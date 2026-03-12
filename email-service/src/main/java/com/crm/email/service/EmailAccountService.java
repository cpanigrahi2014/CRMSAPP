package com.crm.email.service;

import com.crm.common.dto.ApiResponse;
import com.crm.email.dto.*;
import com.crm.email.entity.EmailAccount;
import com.crm.email.repository.EmailAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages connected email accounts (Gmail, Outlook, SMTP).
 * Provides OAuth2 authorization URL generation and token exchange stubs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAccountService {

    private final EmailAccountRepository repo;

    @Value("${app.oauth2.gmail.client-id:}")     private String gmailClientId;
    @Value("${app.oauth2.gmail.client-secret:}") private String gmailClientSecret;
    @Value("${app.oauth2.gmail.redirect-uri:}")  private String gmailRedirectUri;

    @Value("${app.oauth2.outlook.client-id:}")     private String outlookClientId;
    @Value("${app.oauth2.outlook.client-secret:}") private String outlookClientSecret;
    @Value("${app.oauth2.outlook.tenant-id:common}") private String outlookTenantId;
    @Value("${app.oauth2.outlook.redirect-uri:}")  private String outlookRedirectUri;

    /* ── List accounts ────────────────────────────────────────── */
    public List<EmailAccountDto> listAccounts(String tenantId) {
        return repo.findByTenantIdAndDeletedFalse(tenantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /* ── Get single account ───────────────────────────────────── */
    public EmailAccountDto getAccount(String tenantId, UUID id) {
        return repo.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Email account not found"));
    }

    /* ── Create SMTP account ──────────────────────────────────── */
    @Transactional
    public EmailAccountDto createSmtpAccount(String tenantId, String userId, CreateEmailAccountRequest req) {
        EmailAccount account = new EmailAccount();
        account.setTenantId(tenantId);
        account.setCreatedBy(userId);
        account.setProvider(EmailAccount.Provider.SMTP);
        account.setEmail(req.getEmail());
        account.setDisplayName(req.getDisplayName());
        account.setSmtpHost(req.getSmtpHost());
        account.setSmtpPort(req.getSmtpPort());
        account.setSmtpUsername(req.getSmtpUsername());
        account.setSmtpPassword(req.getSmtpPassword());
        account.setDefault(req.isDefault());
        account.setConnected(true);
        return toDto(repo.save(account));
    }

    /* ── Gmail OAuth2 – generate authorization URL ────────────── */
    public OAuthConnectResponse getGmailAuthUrl(String tenantId) {
        if (gmailClientId.isBlank()) {
            throw new RuntimeException("Gmail OAuth2 not configured. Set GMAIL_CLIENT_ID and GMAIL_CLIENT_SECRET.");
        }
        String state = UUID.randomUUID().toString();
        String url = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + enc(gmailClientId) +
                "&redirect_uri=" + enc(gmailRedirectUri) +
                "&response_type=code" +
                "&scope=" + enc("https://www.googleapis.com/auth/gmail.send https://www.googleapis.com/auth/gmail.readonly https://www.googleapis.com/auth/gmail.modify") +
                "&access_type=offline" +
                "&prompt=consent" +
                "&state=" + state;
        return OAuthConnectResponse.builder()
                .authorizationUrl(url).provider("GMAIL").state(state).build();
    }

    /* ── Gmail OAuth2 – exchange code for tokens ──────────────── */
    @Transactional
    public EmailAccountDto connectGmail(String tenantId, String userId, String authCode) {
        // In production, exchange authCode for access/refresh tokens via Google's token endpoint
        // POST https://oauth2.googleapis.com/token with code, client_id, client_secret, redirect_uri, grant_type=authorization_code
        log.info("Exchanging Gmail auth code for tokens (tenant: {})", tenantId);

        EmailAccount account = new EmailAccount();
        account.setTenantId(tenantId);
        account.setCreatedBy(userId);
        account.setProvider(EmailAccount.Provider.GMAIL);
        account.setEmail(userId); // Would come from Google's userinfo endpoint
        account.setDisplayName("Gmail Account");
        account.setAccessToken("gmail-access-token-placeholder");
        account.setRefreshToken("gmail-refresh-token-placeholder");
        account.setConnected(true);
        return toDto(repo.save(account));
    }

    /* ── Outlook OAuth2 – generate authorization URL ──────────── */
    public OAuthConnectResponse getOutlookAuthUrl(String tenantId) {
        if (outlookClientId.isBlank()) {
            throw new RuntimeException("Outlook OAuth2 not configured. Set OUTLOOK_CLIENT_ID and OUTLOOK_CLIENT_SECRET.");
        }
        String state = UUID.randomUUID().toString();
        String url = "https://login.microsoftonline.com/" + outlookTenantId + "/oauth2/v2.0/authorize?" +
                "client_id=" + enc(outlookClientId) +
                "&redirect_uri=" + enc(outlookRedirectUri) +
                "&response_type=code" +
                "&scope=" + enc("openid profile email Mail.Send Mail.ReadWrite offline_access") +
                "&state=" + state;
        return OAuthConnectResponse.builder()
                .authorizationUrl(url).provider("OUTLOOK").state(state).build();
    }

    /* ── Outlook OAuth2 – exchange code ───────────────────────── */
    @Transactional
    public EmailAccountDto connectOutlook(String tenantId, String userId, String authCode) {
        // In production: POST https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token
        log.info("Exchanging Outlook auth code for tokens (tenant: {})", tenantId);

        EmailAccount account = new EmailAccount();
        account.setTenantId(tenantId);
        account.setCreatedBy(userId);
        account.setProvider(EmailAccount.Provider.OUTLOOK);
        account.setEmail(userId);
        account.setDisplayName("Outlook Account");
        account.setAccessToken("outlook-access-token-placeholder");
        account.setRefreshToken("outlook-refresh-token-placeholder");
        account.setConnected(true);
        return toDto(repo.save(account));
    }

    /* ── Disconnect account ───────────────────────────────────── */
    @Transactional
    public void disconnect(String tenantId, UUID id) {
        EmailAccount account = repo.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Email account not found"));
        account.setConnected(false);
        account.setAccessToken(null);
        account.setRefreshToken(null);
        repo.save(account);
    }

    /* ── Delete account ───────────────────────────────────────── */
    @Transactional
    public void deleteAccount(String tenantId, UUID id) {
        EmailAccount account = repo.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Email account not found"));
        account.setDeleted(true);
        repo.save(account);
    }

    /* ── Set default ──────────────────────────────────────────── */
    @Transactional
    public EmailAccountDto setDefault(String tenantId, UUID id) {
        // Clear existing default
        repo.findByTenantIdAndIsDefaultTrueAndDeletedFalse(tenantId)
                .ifPresent(a -> { a.setDefault(false); repo.save(a); });
        EmailAccount account = repo.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Email account not found"));
        account.setDefault(true);
        return toDto(repo.save(account));
    }

    /* ── Helpers ──────────────────────────────────────────────── */
    EmailAccount getDefaultOrFirst(String tenantId) {
        return repo.findByTenantIdAndIsDefaultTrueAndDeletedFalse(tenantId)
                .orElseGet(() -> repo.findByTenantIdAndDeletedFalse(tenantId)
                        .stream().filter(EmailAccount::isConnected).findFirst()
                        .orElse(null));
    }

    private EmailAccountDto toDto(EmailAccount a) {
        return EmailAccountDto.builder()
                .id(a.getId()).provider(a.getProvider().name())
                .email(a.getEmail()).displayName(a.getDisplayName())
                .isDefault(a.isDefault()).connected(a.isConnected())
                .lastSyncAt(a.getLastSyncAt()).createdAt(a.getCreatedAt())
                .smtpHost(a.getSmtpHost()).smtpPort(a.getSmtpPort())
                .build();
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
