package com.crm.account.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.account.dto.*;
import com.crm.account.entity.*;
import com.crm.account.mapper.AccountMapper;
import com.crm.account.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountNoteRepository accountNoteRepository;
    private final AccountTagRepository accountTagRepository;
    private final AccountAttachmentRepository accountAttachmentRepository;
    private final AccountActivityRepository accountActivityRepository;
    private final AccountMapper accountMapper;
    private final EventPublisher eventPublisher;

    // ── 1. Create Account ──────────────────────────────────────
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public AccountResponse createAccount(CreateAccountRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating account for tenant: {}", tenantId);

        Account account = accountMapper.toEntity(request);
        account.setTenantId(tenantId);
        if (account.getType() == null) account.setType("PROSPECT");
        if (account.getLifecycleStage() == null) account.setLifecycleStage("NEW");
        if (account.getHealthScore() == null) account.setHealthScore(50);
        if (account.getEngagementScore() == null) account.setEngagementScore(0);

        Account saved = accountRepository.save(account);
        log.info("Account created: {} for tenant: {}", saved.getId(), tenantId);

        logActivity(saved.getId(), tenantId, "CREATED", "Account created: " + saved.getName(), userId);

        eventPublisher.publish("account-events", tenantId, userId, "Account",
                saved.getId().toString(), "ACCOUNT_CREATED", toResponseWithTags(saved, tenantId));

        return toResponseWithTags(saved, tenantId);
    }

    // ── 2. Edit Account ────────────────────────────────────────
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public AccountResponse updateAccount(UUID accountId, UpdateAccountRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Updating account: {} for tenant: {}", accountId, tenantId);

        Account account = findAccountOrThrow(accountId, tenantId);

        if (request.getName() != null) account.setName(request.getName());
        if (request.getIndustry() != null) account.setIndustry(request.getIndustry());
        if (request.getWebsite() != null) account.setWebsite(request.getWebsite());
        if (request.getPhone() != null) account.setPhone(request.getPhone());
        if (request.getBillingAddress() != null) account.setBillingAddress(request.getBillingAddress());
        if (request.getShippingAddress() != null) account.setShippingAddress(request.getShippingAddress());
        if (request.getAnnualRevenue() != null) account.setAnnualRevenue(request.getAnnualRevenue());
        if (request.getNumberOfEmployees() != null) account.setNumberOfEmployees(request.getNumberOfEmployees());
        if (request.getParentAccountId() != null) account.setParentAccountId(request.getParentAccountId());
        if (request.getDescription() != null) account.setDescription(request.getDescription());
        if (request.getType() != null) account.setType(request.getType());
        if (request.getOwnerId() != null) account.setOwnerId(request.getOwnerId());
        if (request.getTerritory() != null) account.setTerritory(request.getTerritory());
        if (request.getLifecycleStage() != null) account.setLifecycleStage(request.getLifecycleStage());
        if (request.getSegment() != null) account.setSegment(request.getSegment());
        if (request.getHealthScore() != null) account.setHealthScore(request.getHealthScore());
        if (request.getEngagementScore() != null) account.setEngagementScore(request.getEngagementScore());

        Account updated = accountRepository.save(account);
        log.info("Account updated: {}", accountId);

        logActivity(accountId, tenantId, "UPDATED", "Account updated", userId);

        eventPublisher.publish("account-events", tenantId, userId, "Account",
                updated.getId().toString(), "ACCOUNT_UPDATED", toResponseWithTags(updated, tenantId));

        return toResponseWithTags(updated, tenantId);
    }

    // ── 3. Delete Account ──────────────────────────────────────
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public void deleteAccount(UUID accountId, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Soft deleting account: {}", accountId);

        Account account = findAccountOrThrow(accountId, tenantId);
        account.setDeleted(true);
        accountRepository.save(account);

        logActivity(accountId, tenantId, "DELETED", "Account deleted", userId);

        eventPublisher.publish("account-events", tenantId, userId, "Account",
                account.getId().toString(), "ACCOUNT_DELETED", null);
    }

    // ── Get by ID ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    @Cacheable(value = "accounts", key = "#accountId + '_' + T(com.crm.common.security.TenantContext).getTenantId()")
    public AccountResponse getAccountById(UUID accountId) {
        String tenantId = TenantContext.getTenantId();
        Account account = findAccountOrThrow(accountId, tenantId);
        return toResponseWithTags(account, tenantId);
    }

    // ── Get all accounts ───────────────────────────────────────
    @Transactional(readOnly = true)
    public PagedResponse<AccountResponse> getAllAccounts(int page, int size, String sortBy, String sortDir) {
        String tenantId = TenantContext.getTenantId();
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Account> accountPage = accountRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        return toPagedResponse(accountPage, tenantId);
    }

    // ── Search accounts ────────────────────────────────────────
    @Transactional(readOnly = true)
    public PagedResponse<AccountResponse> searchAccounts(String query, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Account> accountPage = accountRepository.searchAccounts(tenantId, query, pageable);
        return toPagedResponse(accountPage, tenantId);
    }

    // ── 4. Parent-Child Hierarchy ──────────────────────────────
    @Transactional(readOnly = true)
    public List<AccountResponse> getChildAccounts(UUID parentAccountId) {
        String tenantId = TenantContext.getTenantId();
        findAccountOrThrow(parentAccountId, tenantId);
        List<Account> children = accountRepository.findByParentAccountIdAndTenantIdAndDeletedFalse(parentAccountId, tenantId);
        return children.stream().map(a -> toResponseWithTags(a, tenantId)).toList();
    }

    // ── 5. Account Owner Assignment ────────────────────────────
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public AccountResponse assignOwner(UUID accountId, String ownerId, String userId) {
        String tenantId = TenantContext.getTenantId();
        Account account = findAccountOrThrow(accountId, tenantId);
        account.setOwnerId(ownerId);
        Account saved = accountRepository.save(account);
        logActivity(accountId, tenantId, "OWNER_ASSIGNED", "Owner assigned: " + ownerId, userId);
        return toResponseWithTags(saved, tenantId);
    }

    // ── 6. Account Type Classification ─────────────────────────
    @Transactional(readOnly = true)
    public PagedResponse<AccountResponse> getAccountsByType(String type, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Account> accountPage = accountRepository.findByTypeAndTenantIdAndDeletedFalse(type, tenantId, pageable);
        return toPagedResponse(accountPage, tenantId);
    }

    // ── 8. Account Territory Assignment ────────────────────────
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public AccountResponse assignTerritory(UUID accountId, String territory, String userId) {
        String tenantId = TenantContext.getTenantId();
        Account account = findAccountOrThrow(accountId, tenantId);
        account.setTerritory(territory);
        Account saved = accountRepository.save(account);
        logActivity(accountId, tenantId, "TERRITORY_ASSIGNED", "Territory assigned: " + territory, userId);
        return toResponseWithTags(saved, tenantId);
    }

    // ── 9. Account Notes ───────────────────────────────────────
    @Transactional
    public AccountNoteResponse addNote(UUID accountId, AccountNoteRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        findAccountOrThrow(accountId, tenantId);

        AccountNote note = AccountNote.builder()
                .accountId(accountId)
                .content(request.getContent())
                .build();
        note.setTenantId(tenantId);
        AccountNote saved = accountNoteRepository.save(note);

        logActivity(accountId, tenantId, "NOTE_ADDED", "Note added", userId);
        return accountMapper.toNoteResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountNoteResponse> getNotes(UUID accountId) {
        String tenantId = TenantContext.getTenantId();
        findAccountOrThrow(accountId, tenantId);
        return accountNoteRepository.findByAccountIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(accountId, tenantId)
                .stream().map(accountMapper::toNoteResponse).toList();
    }

    @Transactional
    public void deleteNote(UUID noteId) {
        String tenantId = TenantContext.getTenantId();
        AccountNote note = accountNoteRepository.findByIdAndTenantIdAndDeletedFalse(noteId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("AccountNote", "id", noteId));
        note.setDeleted(true);
        accountNoteRepository.save(note);
    }

    // ── 10. Account Attachments ────────────────────────────────
    @Transactional
    public AccountAttachmentResponse addAttachment(UUID accountId, AccountAttachmentRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        findAccountOrThrow(accountId, tenantId);

        AccountAttachment attachment = AccountAttachment.builder()
                .accountId(accountId)
                .fileName(request.getFileName())
                .fileUrl(request.getFileUrl())
                .fileSize(request.getFileSize())
                .fileType(request.getFileType())
                .build();
        attachment.setTenantId(tenantId);
        AccountAttachment saved = accountAttachmentRepository.save(attachment);

        logActivity(accountId, tenantId, "ATTACHMENT_ADDED", "File attached: " + request.getFileName(), userId);
        return accountMapper.toAttachmentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountAttachmentResponse> getAttachments(UUID accountId) {
        String tenantId = TenantContext.getTenantId();
        findAccountOrThrow(accountId, tenantId);
        return accountAttachmentRepository.findByAccountIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(accountId, tenantId)
                .stream().map(accountMapper::toAttachmentResponse).toList();
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId) {
        String tenantId = TenantContext.getTenantId();
        AccountAttachment attachment = accountAttachmentRepository.findByIdAndTenantIdAndDeletedFalse(attachmentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("AccountAttachment", "id", attachmentId));
        attachment.setDeleted(true);
        accountAttachmentRepository.save(attachment);
    }

    // ── 11. Account Activity Timeline ──────────────────────────
    @Transactional(readOnly = true)
    public List<AccountActivityResponse> getActivities(UUID accountId) {
        String tenantId = TenantContext.getTenantId();
        findAccountOrThrow(accountId, tenantId);
        return accountActivityRepository.findByAccountIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(accountId, tenantId)
                .stream().map(accountMapper::toActivityResponse).toList();
    }

    // ── 16. Account Import (CSV) ──────────────────────────────
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public int importAccountsFromCsv(String csvContent, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Importing accounts from CSV for tenant: {}", tenantId);

        String[] lines = csvContent.split("\n");
        if (lines.length < 2) return 0;

        String[] headers = lines[0].trim().split(",");
        int imported = 0;

        for (int i = 1; i < lines.length; i++) {
            try {
                String[] vals = lines[i].trim().split(",", -1);
                if (vals.length < 1 || vals[0].isBlank()) continue;

                Account account = new Account();
                account.setTenantId(tenantId);
                for (int j = 0; j < headers.length && j < vals.length; j++) {
                    String h = headers[j].trim().toLowerCase();
                    String v = vals[j].trim();
                    if (v.isEmpty()) continue;
                    switch (h) {
                        case "name" -> account.setName(v);
                        case "industry" -> account.setIndustry(v);
                        case "website" -> account.setWebsite(v);
                        case "phone" -> account.setPhone(v);
                        case "type" -> account.setType(v);
                        case "territory" -> account.setTerritory(v);
                        case "segment" -> account.setSegment(v);
                        case "lifecycle_stage", "lifecyclestage" -> account.setLifecycleStage(v);
                        case "billing_address", "billingaddress" -> account.setBillingAddress(v);
                        case "shipping_address", "shippingaddress" -> account.setShippingAddress(v);
                        case "annual_revenue", "annualrevenue" -> account.setAnnualRevenue(new BigDecimal(v));
                        case "number_of_employees", "numberofemployees", "employees" -> account.setNumberOfEmployees(Integer.parseInt(v));
                        case "description" -> account.setDescription(v);
                    }
                }
                if (account.getName() == null || account.getName().isBlank()) continue;
                if (account.getType() == null) account.setType("PROSPECT");
                if (account.getLifecycleStage() == null) account.setLifecycleStage("NEW");
                if (account.getHealthScore() == null) account.setHealthScore(50);
                if (account.getEngagementScore() == null) account.setEngagementScore(0);
                accountRepository.save(account);
                imported++;
            } catch (Exception e) {
                log.warn("Skipping CSV row {}: {}", i, e.getMessage());
            }
        }
        log.info("Imported {} accounts", imported);
        return imported;
    }

    // ── 17. Account Export (CSV) ──────────────────────────────
    @Transactional(readOnly = true)
    public String exportAccountsToCsv() {
        String tenantId = TenantContext.getTenantId();
        List<Account> accounts = accountRepository.findByTenantIdAndDeletedFalse(tenantId);
        StringBuilder sb = new StringBuilder();
        sb.append("name,industry,website,phone,type,territory,segment,lifecycle_stage,billing_address,shipping_address,annual_revenue,employees,health_score,engagement_score,description\n");
        for (Account a : accounts) {
            sb.append(csvEscape(a.getName())).append(",")
              .append(csvEscape(a.getIndustry())).append(",")
              .append(csvEscape(a.getWebsite())).append(",")
              .append(csvEscape(a.getPhone())).append(",")
              .append(csvEscape(a.getType())).append(",")
              .append(csvEscape(a.getTerritory())).append(",")
              .append(csvEscape(a.getSegment())).append(",")
              .append(csvEscape(a.getLifecycleStage())).append(",")
              .append(csvEscape(a.getBillingAddress())).append(",")
              .append(csvEscape(a.getShippingAddress())).append(",")
              .append(a.getAnnualRevenue() != null ? a.getAnnualRevenue() : "").append(",")
              .append(a.getNumberOfEmployees() != null ? a.getNumberOfEmployees() : "").append(",")
              .append(a.getHealthScore() != null ? a.getHealthScore() : "").append(",")
              .append(a.getEngagementScore() != null ? a.getEngagementScore() : "").append(",")
              .append(csvEscape(a.getDescription())).append("\n");
        }
        return sb.toString();
    }

    // ── 18. Account Tagging ───────────────────────────────────
    @Transactional
    public AccountTagResponse createTag(String name, String color) {
        String tenantId = TenantContext.getTenantId();
        Optional<AccountTag> existing = accountTagRepository.findByNameAndTenantIdAndDeletedFalse(name, tenantId);
        if (existing.isPresent()) return accountMapper.toTagResponse(existing.get());

        AccountTag tag = AccountTag.builder().name(name).color(color != null ? color : "#1976d2").build();
        tag.setTenantId(tenantId);
        return accountMapper.toTagResponse(accountTagRepository.save(tag));
    }

    @Transactional(readOnly = true)
    public List<AccountTagResponse> getAllTags() {
        String tenantId = TenantContext.getTenantId();
        return accountTagRepository.findByTenantIdAndDeletedFalse(tenantId)
                .stream().map(accountMapper::toTagResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AccountTagResponse> getAccountTags(UUID accountId) {
        String tenantId = TenantContext.getTenantId();
        findAccountOrThrow(accountId, tenantId);
        return accountTagRepository.findTagsByAccountId(accountId, tenantId)
                .stream().map(accountMapper::toTagResponse).toList();
    }

    @Transactional
    public void addTagToAccount(UUID accountId, UUID tagId, String userId) {
        String tenantId = TenantContext.getTenantId();
        findAccountOrThrow(accountId, tenantId);
        accountTagRepository.findByIdAndTenantIdAndDeletedFalse(tagId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("AccountTag", "id", tagId));
        accountTagRepository.addTagToAccount(accountId, tagId);
        logActivity(accountId, tenantId, "TAG_ADDED", "Tag added", userId);
    }

    @Transactional
    public void removeTagFromAccount(UUID accountId, UUID tagId) {
        accountTagRepository.removeTagFromAccount(accountId, tagId);
    }

    // ── 19. Account Duplicate Detection ───────────────────────
    @Transactional(readOnly = true)
    public List<AccountResponse> detectDuplicates(String name, String phone, String website) {
        String tenantId = TenantContext.getTenantId();
        return accountRepository.findPotentialDuplicates(tenantId,
                        name != null ? name : "",
                        phone != null ? phone : "",
                        website != null ? website : "")
                .stream().map(a -> toResponseWithTags(a, tenantId)).toList();
    }

    // ── 20. Account Segmentation ──────────────────────────────
    @Transactional(readOnly = true)
    public PagedResponse<AccountResponse> getAccountsBySegment(String segment, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Account> accountPage = accountRepository.findBySegmentAndTenantIdAndDeletedFalse(segment, tenantId, pageable);
        return toPagedResponse(accountPage, tenantId);
    }

    // ── 21. Account Lifecycle Stage ───────────────────────────
    @Transactional(readOnly = true)
    public PagedResponse<AccountResponse> getAccountsByLifecycleStage(String stage, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Account> accountPage = accountRepository.findByLifecycleStageAndTenantIdAndDeletedFalse(stage, tenantId, pageable);
        return toPagedResponse(accountPage, tenantId);
    }

    // ── 22. Account Health Score ──────────────────────────────
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public AccountResponse updateHealthScore(UUID accountId, int healthScore, String userId) {
        String tenantId = TenantContext.getTenantId();
        Account account = findAccountOrThrow(accountId, tenantId);
        account.setHealthScore(Math.max(0, Math.min(100, healthScore)));
        Account saved = accountRepository.save(account);
        logActivity(accountId, tenantId, "HEALTH_SCORE_UPDATED", "Health score: " + healthScore, userId);
        return toResponseWithTags(saved, tenantId);
    }

    // ── 23. Account Engagement Tracking ───────────────────────
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public AccountResponse updateEngagementScore(UUID accountId, int engagementScore, String userId) {
        String tenantId = TenantContext.getTenantId();
        Account account = findAccountOrThrow(accountId, tenantId);
        account.setEngagementScore(Math.max(0, Math.min(100, engagementScore)));
        Account saved = accountRepository.save(account);
        logActivity(accountId, tenantId, "ENGAGEMENT_UPDATED", "Engagement score: " + engagementScore, userId);
        return toResponseWithTags(saved, tenantId);
    }

    // ── 24 & 25. Account Analytics / Reporting / Dashboard ────
    @Transactional(readOnly = true)
    public AccountAnalyticsResponse getAnalytics() {
        String tenantId = TenantContext.getTenantId();

        long total = accountRepository.countByTenantIdAndDeletedFalse(tenantId);
        long active = accountRepository.countByLifecycleStageAndTenantIdAndDeletedFalse("ACTIVE", tenantId);
        long newAccounts = accountRepository.countByLifecycleStageAndTenantIdAndDeletedFalse("NEW", tenantId);
        long churned = accountRepository.countByLifecycleStageAndTenantIdAndDeletedFalse("CHURNED", tenantId);
        BigDecimal totalRevenue = accountRepository.sumAnnualRevenue(tenantId);
        BigDecimal avgRevenue = accountRepository.avgAnnualRevenue(tenantId);
        Double avgHealth = accountRepository.avgHealthScore(tenantId);
        Double avgEngagement = accountRepository.avgEngagementScore(tenantId);

        Map<String, Long> byType = groupedToMap(accountRepository.countByTypeGrouped(tenantId));
        Map<String, Long> byIndustry = groupedToMap(accountRepository.countByIndustryGrouped(tenantId));
        Map<String, Long> byLifecycle = groupedToMap(accountRepository.countByLifecycleStageGrouped(tenantId));
        Map<String, Long> byTerritory = groupedToMap(accountRepository.countByTerritoryGrouped(tenantId));
        Map<String, Long> bySegment = groupedToMap(accountRepository.countBySegmentGrouped(tenantId));

        return AccountAnalyticsResponse.builder()
                .totalAccounts(total)
                .activeAccounts(active)
                .newAccounts(newAccounts)
                .churnedAccounts(churned)
                .totalRevenue(totalRevenue)
                .averageRevenue(avgRevenue)
                .averageHealthScore(avgHealth != null ? avgHealth : 0)
                .averageEngagementScore(avgEngagement != null ? avgEngagement : 0)
                .byType(byType)
                .byIndustry(byIndustry)
                .byLifecycleStage(byLifecycle)
                .byTerritory(byTerritory)
                .bySegment(bySegment)
                .build();
    }

    // ── Bulk Operations ───────────────────────────────────────
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public int bulkUpdate(BulkAccountUpdateRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        List<UUID> ids = request.getAccountIds().stream().map(UUID::fromString).toList();
        List<Account> accounts = accountRepository.findByIdInAndTenantIdAndDeletedFalse(ids, tenantId);

        for (Account account : accounts) {
            if (request.getOwnerId() != null) account.setOwnerId(request.getOwnerId());
            if (request.getTerritory() != null) account.setTerritory(request.getTerritory());
            if (request.getType() != null) account.setType(request.getType());
            if (request.getLifecycleStage() != null) account.setLifecycleStage(request.getLifecycleStage());
            if (request.getSegment() != null) account.setSegment(request.getSegment());
        }
        accountRepository.saveAll(accounts);
        log.info("Bulk updated {} accounts", accounts.size());
        return accounts.size();
    }

    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public int bulkDelete(List<String> accountIds, String userId) {
        String tenantId = TenantContext.getTenantId();
        List<UUID> ids = accountIds.stream().map(UUID::fromString).toList();
        List<Account> accounts = accountRepository.findByIdInAndTenantIdAndDeletedFalse(ids, tenantId);
        accounts.forEach(a -> a.setDeleted(true));
        accountRepository.saveAll(accounts);
        log.info("Bulk deleted {} accounts", accounts.size());
        return accounts.size();
    }

    // ── Get by territory ──────────────────────────────────────
    @Transactional(readOnly = true)
    public PagedResponse<AccountResponse> getAccountsByTerritory(String territory, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Account> accountPage = accountRepository.findByTerritoryAndTenantIdAndDeletedFalse(territory, tenantId, pageable);
        return toPagedResponse(accountPage, tenantId);
    }

    // ── Get by owner ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public PagedResponse<AccountResponse> getAccountsByOwner(String ownerId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Account> accountPage = accountRepository.findByOwnerIdAndTenantIdAndDeletedFalse(ownerId, tenantId, pageable);
        return toPagedResponse(accountPage, tenantId);
    }

    // ── Helpers ───────────────────────────────────────────────
    private Account findAccountOrThrow(UUID accountId, String tenantId) {
        return accountRepository.findByIdAndTenantIdAndDeletedFalse(accountId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
    }

    private AccountResponse toResponseWithTags(Account account, String tenantId) {
        AccountResponse response = accountMapper.toResponse(account);
        List<AccountTagResponse> tags = accountTagRepository.findTagsByAccountId(account.getId(), tenantId)
                .stream().map(accountMapper::toTagResponse).toList();
        response.setTags(tags);
        return response;
    }

    private PagedResponse<AccountResponse> toPagedResponse(Page<Account> page, String tenantId) {
        return PagedResponse.<AccountResponse>builder()
                .content(page.getContent().stream().map(a -> toResponseWithTags(a, tenantId)).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    private void logActivity(UUID accountId, String tenantId, String type, String description, String userId) {
        try {
            AccountActivity activity = AccountActivity.builder()
                    .accountId(accountId)
                    .type(type)
                    .description(description)
                    .performedBy(userId)
                    .build();
            activity.setTenantId(tenantId);
            accountActivityRepository.save(activity);
        } catch (Exception e) {
            log.warn("Failed to log activity: {}", e.getMessage());
        }
    }

    private Map<String, Long> groupedToMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] != null ? row[0].toString() : "Unknown";
            Long count = ((Number) row[1]).longValue();
            map.put(key, count);
        }
        return map;
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
