package com.crm.contact.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.contact.dto.*;
import com.crm.contact.entity.*;
import com.crm.contact.mapper.ContactMapper;
import com.crm.contact.repository.*;
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

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final ContactTagRepository tagRepository;
    private final ContactCommunicationRepository communicationRepository;
    private final ContactActivityRepository activityRepository;
    private final ContactNoteRepository noteRepository;
    private final ContactAttachmentRepository attachmentRepository;
    private final ContactMapper contactMapper;
    private final EventPublisher eventPublisher;

    // ═══════════════════════════════════════════════════════════
    // Feature 1: Contact creation
    // ═══════════════════════════════════════════════════════════
    @Transactional
    @CacheEvict(value = "contacts", allEntries = true)
    public ContactResponse createContact(CreateContactRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating contact for tenant: {}", tenantId);

        Contact contact = contactMapper.toEntity(request);
        contact.setTenantId(tenantId);

        // Handle consent defaults
        if (request.getEmailOptIn() != null) contact.setEmailOptIn(request.getEmailOptIn());
        if (request.getSmsOptIn() != null) contact.setSmsOptIn(request.getSmsOptIn());
        if (request.getPhoneOptIn() != null) contact.setPhoneOptIn(request.getPhoneOptIn());
        if (request.getDoNotCall() != null) contact.setDoNotCall(request.getDoNotCall());
        if (request.getConsentSource() != null) {
            contact.setConsentSource(request.getConsentSource());
            contact.setConsentDate(LocalDateTime.now());
        }

        Contact savedContact = contactRepository.save(contact);
        log.info("Contact created: {} for tenant: {}", savedContact.getId(), tenantId);

        // Record activity
        recordActivity(savedContact.getId(), tenantId, "CREATED",
                "Contact created: " + savedContact.getFirstName() + " " + savedContact.getLastName(), userId);

        eventPublisher.publish("contact-events", tenantId, userId, "Contact",
                savedContact.getId().toString(), "CONTACT_CREATED", contactMapper.toResponse(savedContact));

        return contactMapper.toResponse(savedContact);
    }

    // ═══════════════════════════════════════════════════════════
    // Feature 1 + 2: Contact update (includes account linking)
    // ═══════════════════════════════════════════════════════════
    @Transactional
    @CacheEvict(value = "contacts", allEntries = true)
    public ContactResponse updateContact(UUID contactId, UpdateContactRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Updating contact: {} for tenant: {}", contactId, tenantId);

        Contact contact = contactRepository.findByIdAndTenantIdAndDeletedFalse(contactId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", contactId));

        // Core fields
        if (request.getFirstName() != null) contact.setFirstName(request.getFirstName());
        if (request.getLastName() != null) contact.setLastName(request.getLastName());
        if (request.getEmail() != null) contact.setEmail(request.getEmail());
        if (request.getPhone() != null) contact.setPhone(request.getPhone());
        if (request.getMobilePhone() != null) contact.setMobilePhone(request.getMobilePhone());
        if (request.getTitle() != null) contact.setTitle(request.getTitle());
        if (request.getDepartment() != null) contact.setDepartment(request.getDepartment());
        if (request.getAccountId() != null) contact.setAccountId(request.getAccountId());
        if (request.getMailingAddress() != null) contact.setMailingAddress(request.getMailingAddress());
        if (request.getDescription() != null) contact.setDescription(request.getDescription());
        if (request.getOwnerId() != null) contact.setOwnerId(request.getOwnerId());

        // Social profiles (Feature 4)
        if (request.getLinkedinUrl() != null) contact.setLinkedinUrl(request.getLinkedinUrl());
        if (request.getTwitterUrl() != null) contact.setTwitterUrl(request.getTwitterUrl());
        if (request.getFacebookUrl() != null) contact.setFacebookUrl(request.getFacebookUrl());
        if (request.getOtherSocialUrl() != null) contact.setOtherSocialUrl(request.getOtherSocialUrl());

        // Segmentation (Feature 5)
        if (request.getLeadSource() != null) contact.setLeadSource(request.getLeadSource());
        if (request.getLifecycleStage() != null) contact.setLifecycleStage(request.getLifecycleStage());
        if (request.getSegment() != null) contact.setSegment(request.getSegment());

        // Consent (Feature 6)
        if (request.getEmailOptIn() != null) contact.setEmailOptIn(request.getEmailOptIn());
        if (request.getSmsOptIn() != null) contact.setSmsOptIn(request.getSmsOptIn());
        if (request.getPhoneOptIn() != null) contact.setPhoneOptIn(request.getPhoneOptIn());
        if (request.getDoNotCall() != null) contact.setDoNotCall(request.getDoNotCall());
        if (request.getConsentSource() != null) {
            contact.setConsentSource(request.getConsentSource());
            contact.setConsentDate(LocalDateTime.now());
        }

        Contact updatedContact = contactRepository.save(contact);
        log.info("Contact updated: {}", contactId);

        recordActivity(contactId, tenantId, "UPDATED", "Contact updated", userId);

        eventPublisher.publish("contact-events", tenantId, userId, "Contact",
                updatedContact.getId().toString(), "CONTACT_UPDATED", contactMapper.toResponse(updatedContact));

        return contactMapper.toResponse(updatedContact);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "contacts", key = "#contactId + '_' + T(com.crm.common.security.TenantContext).getTenantId()")
    public ContactResponse getContactById(UUID contactId) {
        String tenantId = TenantContext.getTenantId();
        log.debug("Fetching contact: {} for tenant: {}", contactId, tenantId);

        Contact contact = contactRepository.findByIdAndTenantIdAndDeletedFalse(contactId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", contactId));

        return contactMapper.toResponse(contact);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ContactResponse> getAllContacts(int page, int size, String sortBy, String sortDir) {
        String tenantId = TenantContext.getTenantId();
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Contact> contactPage = contactRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);

        return buildPagedResponse(contactPage);
    }

    // ═══════════════════════════════════════════════════════════
    // Feature 2: Contact linking to accounts
    // ═══════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public PagedResponse<ContactResponse> getContactsByAccount(UUID accountId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        log.debug("Fetching contacts for account: {} tenant: {}", accountId, tenantId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Contact> contactPage = contactRepository.findByAccountIdAndTenantIdAndDeletedFalse(accountId, tenantId, pageable);

        return buildPagedResponse(contactPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ContactResponse> searchContacts(String query, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size);

        Page<Contact> contactPage = contactRepository.searchContacts(tenantId, query, pageable);

        return buildPagedResponse(contactPage);
    }

    @Transactional
    @CacheEvict(value = "contacts", allEntries = true)
    public void deleteContact(UUID contactId, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Soft deleting contact: {}", contactId);

        Contact contact = contactRepository.findByIdAndTenantIdAndDeletedFalse(contactId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", contactId));

        contact.setDeleted(true);
        contactRepository.save(contact);

        recordActivity(contactId, tenantId, "DELETED", "Contact deleted", userId);

        eventPublisher.publish("contact-events", tenantId, userId, "Contact",
                contact.getId().toString(), "CONTACT_DELETED", null);
    }

    // ═══════════════════════════════════════════════════════════
    // Feature 3: Communication history
    // ═══════════════════════════════════════════════════════════
    @Transactional
    public CommunicationResponse addCommunication(UUID contactId, CreateCommunicationRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        // Verify contact exists
        contactRepository.findByIdAndTenantIdAndDeletedFalse(contactId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", contactId));

        ContactCommunication comm = ContactCommunication.builder()
                .contactId(contactId)
                .commType(request.getCommType())
                .subject(request.getSubject())
                .body(request.getBody())
                .direction(request.getDirection())
                .status(request.getStatus() != null ? request.getStatus() : "COMPLETED")
                .communicationDate(request.getCommunicationDate() != null ? request.getCommunicationDate() : LocalDateTime.now())
                .tenantId(tenantId)
                .createdBy(userId)
                .build();

        ContactCommunication saved = communicationRepository.save(comm);

        recordActivity(contactId, tenantId, "COMMUNICATION_LOGGED",
                request.getCommType() + " – " + (request.getSubject() != null ? request.getSubject() : "No subject"), userId);

        return contactMapper.toCommunicationResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CommunicationResponse> getCommunications(UUID contactId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size);

        Page<ContactCommunication> commPage =
                communicationRepository.findByContactIdAndTenantIdOrderByCommunicationDateDesc(contactId, tenantId, pageable);

        return PagedResponse.<CommunicationResponse>builder()
                .content(commPage.getContent().stream().map(contactMapper::toCommunicationResponse).toList())
                .pageNumber(commPage.getNumber())
                .pageSize(commPage.getSize())
                .totalElements(commPage.getTotalElements())
                .totalPages(commPage.getTotalPages())
                .last(commPage.isLast())
                .first(commPage.isFirst())
                .build();
    }

    @Transactional
    public void deleteCommunication(UUID commId) {
        String tenantId = TenantContext.getTenantId();
        ContactCommunication comm = communicationRepository.findByIdAndTenantId(commId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Communication", "id", commId));
        communicationRepository.delete(comm);
    }

    // ═══════════════════════════════════════════════════════════
    // Feature 5: Segmentation
    // ═══════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public PagedResponse<ContactResponse> getContactsBySegment(String segment, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Contact> contactPage = contactRepository.findBySegmentAndTenantIdAndDeletedFalse(segment, tenantId, pageable);
        return buildPagedResponse(contactPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ContactResponse> getContactsByLifecycleStage(String stage, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Contact> contactPage = contactRepository.findByLifecycleStageAndTenantIdAndDeletedFalse(stage, tenantId, pageable);
        return buildPagedResponse(contactPage);
    }

    // ═══════════════════════════════════════════════════════════
    // Feature 6: Marketing consent tracking
    // ═══════════════════════════════════════════════════════════
    @Transactional
    @CacheEvict(value = "contacts", allEntries = true)
    public ContactResponse updateConsent(UUID contactId, UpdateConsentRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        Contact contact = contactRepository.findByIdAndTenantIdAndDeletedFalse(contactId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", contactId));

        if (request.getEmailOptIn() != null) contact.setEmailOptIn(request.getEmailOptIn());
        if (request.getSmsOptIn() != null) contact.setSmsOptIn(request.getSmsOptIn());
        if (request.getPhoneOptIn() != null) contact.setPhoneOptIn(request.getPhoneOptIn());
        if (request.getDoNotCall() != null) contact.setDoNotCall(request.getDoNotCall());
        if (request.getConsentSource() != null) contact.setConsentSource(request.getConsentSource());
        contact.setConsentDate(LocalDateTime.now());

        Contact saved = contactRepository.save(contact);

        recordActivity(contactId, tenantId, "CONSENT_UPDATED",
                "Consent updated – email:" + contact.isEmailOptIn() + " sms:" + contact.isSmsOptIn(), userId);

        return contactMapper.toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════
    // Feature 7: Activity timeline
    // ═══════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public PagedResponse<ContactActivityResponse> getActivityTimeline(UUID contactId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size);

        Page<ContactActivity> activityPage =
                activityRepository.findByContactIdAndTenantIdOrderByCreatedAtDesc(contactId, tenantId, pageable);

        return PagedResponse.<ContactActivityResponse>builder()
                .content(activityPage.getContent().stream().map(contactMapper::toActivityResponse).toList())
                .pageNumber(activityPage.getNumber())
                .pageSize(activityPage.getSize())
                .totalElements(activityPage.getTotalElements())
                .totalPages(activityPage.getTotalPages())
                .last(activityPage.isLast())
                .first(activityPage.isFirst())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // Feature 8: Contact tagging
    // ═══════════════════════════════════════════════════════════
    @Transactional
    public ContactTagResponse addTag(UUID contactId, String tagName, String userId) {
        String tenantId = TenantContext.getTenantId();
        // Verify contact exists
        contactRepository.findByIdAndTenantIdAndDeletedFalse(contactId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", contactId));

        // Check if tag already exists
        Optional<ContactTag> existing = tagRepository.findByContactIdAndTagNameAndTenantId(contactId, tagName, tenantId);
        if (existing.isPresent()) {
            return contactMapper.toTagResponse(existing.get());
        }

        ContactTag tag = ContactTag.builder()
                .contactId(contactId)
                .tagName(tagName)
                .tenantId(tenantId)
                .build();

        ContactTag saved = tagRepository.save(tag);

        recordActivity(contactId, tenantId, "TAG_ADDED", "Tag added: " + tagName, userId);

        return contactMapper.toTagResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ContactTagResponse> getTags(UUID contactId) {
        String tenantId = TenantContext.getTenantId();
        return tagRepository.findByContactIdAndTenantId(contactId, tenantId)
                .stream().map(contactMapper::toTagResponse).toList();
    }

    @Transactional
    public void removeTag(UUID contactId, String tagName, String userId) {
        String tenantId = TenantContext.getTenantId();
        tagRepository.deleteByContactIdAndTagNameAndTenantId(contactId, tagName, tenantId);
        recordActivity(contactId, tenantId, "TAG_REMOVED", "Tag removed: " + tagName, userId);
    }

    // ═══════════════════════════════════════════════════════════
    // Feature 9: Duplicate detection
    // ═══════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public List<DuplicateContactGroup> detectDuplicates() {
        String tenantId = TenantContext.getTenantId();
        List<Contact> all = contactRepository.findByTenantIdAndDeletedFalse(tenantId, PageRequest.of(0, 10000)).getContent();

        List<DuplicateContactGroup> groups = new ArrayList<>();

        // Group by email
        Map<String, List<Contact>> byEmail = all.stream()
                .filter(c -> c.getEmail() != null && !c.getEmail().isBlank())
                .collect(Collectors.groupingBy(c -> c.getEmail().toLowerCase()));
        byEmail.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .forEach(e -> groups.add(DuplicateContactGroup.builder()
                        .matchField("email")
                        .matchValue(e.getKey())
                        .contacts(e.getValue().stream().map(contactMapper::toResponse).toList())
                        .build()));

        // Group by phone
        Map<String, List<Contact>> byPhone = all.stream()
                .filter(c -> c.getPhone() != null && !c.getPhone().isBlank())
                .collect(Collectors.groupingBy(Contact::getPhone));
        byPhone.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .forEach(e -> groups.add(DuplicateContactGroup.builder()
                        .matchField("phone")
                        .matchValue(e.getKey())
                        .contacts(e.getValue().stream().map(contactMapper::toResponse).toList())
                        .build()));

        // Group by full name
        Map<String, List<Contact>> byName = all.stream()
                .collect(Collectors.groupingBy(c -> (c.getFirstName() + " " + c.getLastName()).toLowerCase()));
        byName.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .forEach(e -> groups.add(DuplicateContactGroup.builder()
                        .matchField("name")
                        .matchValue(e.getKey())
                        .contacts(e.getValue().stream().map(contactMapper::toResponse).toList())
                        .build()));

        return groups;
    }

    @Transactional
    @CacheEvict(value = "contacts", allEntries = true)
    public ContactResponse mergeContacts(UUID primaryId, UUID duplicateId, String userId) {
        String tenantId = TenantContext.getTenantId();
        Contact primary = contactRepository.findByIdAndTenantIdAndDeletedFalse(primaryId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", primaryId));
        Contact duplicate = contactRepository.findByIdAndTenantIdAndDeletedFalse(duplicateId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", duplicateId));

        // Fill in blanks from duplicate into primary
        if (primary.getPhone() == null && duplicate.getPhone() != null) primary.setPhone(duplicate.getPhone());
        if (primary.getMobilePhone() == null && duplicate.getMobilePhone() != null) primary.setMobilePhone(duplicate.getMobilePhone());
        if (primary.getTitle() == null && duplicate.getTitle() != null) primary.setTitle(duplicate.getTitle());
        if (primary.getDepartment() == null && duplicate.getDepartment() != null) primary.setDepartment(duplicate.getDepartment());
        if (primary.getAccountId() == null && duplicate.getAccountId() != null) primary.setAccountId(duplicate.getAccountId());
        if (primary.getMailingAddress() == null && duplicate.getMailingAddress() != null) primary.setMailingAddress(duplicate.getMailingAddress());
        if (primary.getLinkedinUrl() == null && duplicate.getLinkedinUrl() != null) primary.setLinkedinUrl(duplicate.getLinkedinUrl());
        if (primary.getTwitterUrl() == null && duplicate.getTwitterUrl() != null) primary.setTwitterUrl(duplicate.getTwitterUrl());
        if (primary.getFacebookUrl() == null && duplicate.getFacebookUrl() != null) primary.setFacebookUrl(duplicate.getFacebookUrl());
        if (primary.getSegment() == null && duplicate.getSegment() != null) primary.setSegment(duplicate.getSegment());

        // Move communications from duplicate to primary
        communicationRepository.findByContactIdAndTenantIdOrderByCommunicationDateDesc(duplicateId, tenantId, PageRequest.of(0, 10000))
                .getContent().forEach(c -> { c.setContactId(primaryId); communicationRepository.save(c); });

        // Move tags
        tagRepository.findByContactIdAndTenantId(duplicateId, tenantId).forEach(t -> {
            if (tagRepository.findByContactIdAndTagNameAndTenantId(primaryId, t.getTagName(), tenantId).isEmpty()) {
                t.setContactId(primaryId);
                tagRepository.save(t);
            } else {
                tagRepository.delete(t);
            }
        });

        // Move activities
        activityRepository.findByContactIdAndTenantIdOrderByCreatedAtDesc(duplicateId, tenantId, PageRequest.of(0, 10000))
                .getContent().forEach(a -> { a.setContactId(primaryId); activityRepository.save(a); });

        // Move notes from duplicate to primary
        List<ContactNote> notes = noteRepository.findByContactIdAndDeletedFalse(duplicateId);
        notes.forEach(n -> n.setContactId(primaryId));
        noteRepository.saveAll(notes);

        // Move attachments from duplicate to primary
        List<ContactAttachment> attachments = attachmentRepository.findByContactIdAndDeletedFalse(duplicateId);
        attachments.forEach(a -> a.setContactId(primaryId));
        attachmentRepository.saveAll(attachments);

        // Soft-delete duplicate
        duplicate.setDeleted(true);
        contactRepository.save(duplicate);

        Contact saved = contactRepository.save(primary);

        recordActivity(primaryId, tenantId, "MERGED",
                "Merged with contact " + duplicate.getFirstName() + " " + duplicate.getLastName() + " (" + duplicateId + ")", userId);

        return contactMapper.toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════
    // Feature 10: Contact analytics
    // ═══════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public ContactAnalyticsResponse getAnalytics() {
        String tenantId = TenantContext.getTenantId();

        return ContactAnalyticsResponse.builder()
                .totalContacts(contactRepository.countByTenantIdAndDeletedFalse(tenantId))
                .contactsWithEmail(contactRepository.countByTenantIdAndDeletedFalseAndEmailIsNotNull(tenantId))
                .contactsWithPhone(contactRepository.countByTenantIdAndDeletedFalseAndPhoneIsNotNull(tenantId))
                .contactsWithAccount(contactRepository.countByTenantIdAndDeletedFalseAndAccountIdIsNotNull(tenantId))
                .emailOptInCount(contactRepository.countByTenantIdAndDeletedFalseAndEmailOptInTrue(tenantId))
                .smsOptInCount(contactRepository.countByTenantIdAndDeletedFalseAndSmsOptInTrue(tenantId))
                .doNotCallCount(contactRepository.countByTenantIdAndDeletedFalseAndDoNotCallTrue(tenantId))
                .bySegment(toMap(contactRepository.countBySegment(tenantId)))
                .byLifecycleStage(toMap(contactRepository.countByLifecycleStage(tenantId)))
                .byLeadSource(toMap(contactRepository.countByLeadSource(tenantId)))
                .byDepartment(toMap(contactRepository.countByDepartment(tenantId)))
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // Import / Export
    // ═══════════════════════════════════════════════════════════
    @Transactional
    @CacheEvict(value = "contacts", allEntries = true)
    public Map<String, Object> importContactsFromFile(MultipartFile file, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Importing contacts from CSV for tenant: {}", tenantId);
        int imported = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return Map.of("imported", 0);
            String[] headers = headerLine.trim().split(",");
            Map<String, Integer> colMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colMap.put(headers[i].trim().toLowerCase().replaceAll("[^a-z0-9_]", ""), i);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] vals = line.trim().split(",", -1);
                    if (vals.length < 1 || vals[0].isBlank()) continue;

                    Contact contact = new Contact();
                    contact.setTenantId(tenantId);

                    String firstName = getCsvCol(vals, colMap, "firstname", "first_name");
                    String lastName = getCsvCol(vals, colMap, "lastname", "last_name");
                    if (firstName == null || firstName.isBlank()) continue;
                    contact.setFirstName(firstName);
                    contact.setLastName(lastName != null ? lastName : "");
                    contact.setEmail(getCsvCol(vals, colMap, "email"));
                    contact.setPhone(getCsvCol(vals, colMap, "phone"));
                    contact.setMobilePhone(getCsvCol(vals, colMap, "mobilephone", "mobile_phone", "mobile"));
                    contact.setTitle(getCsvCol(vals, colMap, "title"));
                    contact.setDepartment(getCsvCol(vals, colMap, "department"));
                    contact.setMailingAddress(getCsvCol(vals, colMap, "mailingaddress", "mailing_address", "address"));
                    contact.setDescription(getCsvCol(vals, colMap, "description"));
                    contact.setSegment(getCsvCol(vals, colMap, "segment"));
                    contact.setLifecycleStage(getCsvCol(vals, colMap, "lifecyclestage", "lifecycle_stage"));
                    contact.setLeadSource(getCsvCol(vals, colMap, "leadsource", "lead_source", "source"));

                    contactRepository.save(contact);
                    imported++;
                } catch (Exception e) {
                    log.warn("Skipping CSV row: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file", e);
        }
        log.info("Imported {} contacts", imported);
        return Map.of("imported", imported);
    }

    @Transactional(readOnly = true)
    public String exportContactsToCsv() {
        String tenantId = TenantContext.getTenantId();
        List<Contact> contacts = contactRepository.findByTenantIdAndDeletedFalse(tenantId);
        StringBuilder sb = new StringBuilder();
        sb.append("first_name,last_name,email,phone,mobile_phone,title,department,mailing_address,description,segment,lifecycle_stage,lead_source\n");
        for (Contact c : contacts) {
            sb.append(csvEscape(c.getFirstName())).append(",")
              .append(csvEscape(c.getLastName())).append(",")
              .append(csvEscape(c.getEmail())).append(",")
              .append(csvEscape(c.getPhone())).append(",")
              .append(csvEscape(c.getMobilePhone())).append(",")
              .append(csvEscape(c.getTitle())).append(",")
              .append(csvEscape(c.getDepartment())).append(",")
              .append(csvEscape(c.getMailingAddress())).append(",")
              .append(csvEscape(c.getDescription())).append(",")
              .append(csvEscape(c.getSegment())).append(",")
              .append(csvEscape(c.getLifecycleStage())).append(",")
              .append(csvEscape(c.getLeadSource())).append("\n");
        }
        return sb.toString();
    }

    private String getCsvCol(String[] vals, Map<String, Integer> colMap, String... keys) {
        for (String key : keys) {
            Integer idx = colMap.get(key);
            if (idx != null && idx < vals.length) {
                String v = vals[idx].trim();
                if (!v.isEmpty()) return v;
            }
        }
        return null;
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ═══════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════
    private void recordActivity(UUID contactId, String tenantId, String type, String description, String userId) {
        try {
            ContactActivity activity = ContactActivity.builder()
                    .contactId(contactId)
                    .activityType(type)
                    .description(description)
                    .tenantId(tenantId)
                    .createdBy(userId)
                    .build();
            activityRepository.save(activity);
        } catch (Exception e) {
            log.warn("Failed to record activity for contact {}: {}", contactId, e.getMessage());
        }
    }

    private PagedResponse<ContactResponse> buildPagedResponse(Page<Contact> contactPage) {
        return PagedResponse.<ContactResponse>builder()
                .content(contactPage.getContent().stream().map(contactMapper::toResponse).toList())
                .pageNumber(contactPage.getNumber())
                .pageSize(contactPage.getSize())
                .totalElements(contactPage.getTotalElements())
                .totalPages(contactPage.getTotalPages())
                .last(contactPage.isLast())
                .first(contactPage.isFirst())
                .build();
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put(String.valueOf(row[0]), (Long) row[1]);
        }
        return map;
    }

    // ═══════════════════════════════════════════════════════════
    // Contact Notes
    // ═══════════════════════════════════════════════════════════
    @Transactional
    public ContactNoteResponse addNote(UUID contactId, ContactNoteRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        contactRepository.findByIdAndTenantIdAndDeletedFalse(contactId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", contactId));

        ContactNote note = ContactNote.builder()
                .contactId(contactId)
                .content(request.getContent())
                .build();
        note.setTenantId(tenantId);
        ContactNote saved = noteRepository.save(note);

        recordActivity(contactId, tenantId, "NOTE_ADDED", "Note added", userId);
        return contactMapper.toNoteResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ContactNoteResponse> getNotes(UUID contactId) {
        String tenantId = TenantContext.getTenantId();
        contactRepository.findByIdAndTenantIdAndDeletedFalse(contactId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", contactId));
        return noteRepository.findByContactIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(contactId, tenantId)
                .stream().map(contactMapper::toNoteResponse).toList();
    }

    @Transactional
    public void deleteNote(UUID noteId) {
        String tenantId = TenantContext.getTenantId();
        ContactNote note = noteRepository.findByIdAndTenantIdAndDeletedFalse(noteId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ContactNote", "id", noteId));
        note.setDeleted(true);
        noteRepository.save(note);
    }

    // ═══════════════════════════════════════════════════════════
    // Contact Attachments
    // ═══════════════════════════════════════════════════════════
    @Transactional
    public ContactAttachmentResponse addAttachment(UUID contactId, ContactAttachmentRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        contactRepository.findByIdAndTenantIdAndDeletedFalse(contactId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", contactId));

        ContactAttachment attachment = ContactAttachment.builder()
                .contactId(contactId)
                .fileName(request.getFileName())
                .fileUrl(request.getFileUrl())
                .fileSize(request.getFileSize())
                .fileType(request.getFileType())
                .build();
        attachment.setTenantId(tenantId);
        ContactAttachment saved = attachmentRepository.save(attachment);

        recordActivity(contactId, tenantId, "ATTACHMENT_ADDED", "File attached: " + request.getFileName(), userId);
        return contactMapper.toAttachmentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ContactAttachmentResponse> getAttachments(UUID contactId) {
        String tenantId = TenantContext.getTenantId();
        contactRepository.findByIdAndTenantIdAndDeletedFalse(contactId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", contactId));
        return attachmentRepository.findByContactIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(contactId, tenantId)
                .stream().map(contactMapper::toAttachmentResponse).toList();
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId) {
        String tenantId = TenantContext.getTenantId();
        ContactAttachment attachment = attachmentRepository.findByIdAndTenantIdAndDeletedFalse(attachmentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ContactAttachment", "id", attachmentId));
        attachment.setDeleted(true);
        attachmentRepository.save(attachment);
    }
}
