package com.crm.activity.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.activity.dto.*;
import com.crm.activity.entity.Activity;
import com.crm.activity.mapper.ActivityMapper;
import com.crm.activity.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;
    private final EventPublisher eventPublisher;

    /* ============================================================
       CRUD
       ============================================================ */

    @Transactional
    @CacheEvict(value = "activities", allEntries = true)
    public ActivityResponse createActivity(CreateActivityRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating activity for tenant: {}", tenantId);

        Activity activity = activityMapper.toEntity(request);
        activity.setTenantId(tenantId);
        activity.setStatus(Activity.ActivityStatus.NOT_STARTED);
        if (activity.getPriority() == null) {
            activity.setPriority(Activity.ActivityPriority.MEDIUM);
        }

        Activity savedActivity = activityRepository.save(activity);
        log.info("Activity created: {} for tenant: {}", savedActivity.getId(), tenantId);

        eventPublisher.publish("activity-events", tenantId, userId, "Activity",
                savedActivity.getId().toString(), "ACTIVITY_CREATED", activityMapper.toResponse(savedActivity));

        return activityMapper.toResponse(savedActivity);
    }

    @Transactional
    @CacheEvict(value = "activities", allEntries = true)
    public ActivityResponse updateActivity(UUID activityId, UpdateActivityRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Updating activity: {} for tenant: {}", activityId, tenantId);

        Activity activity = activityRepository.findByIdAndTenantIdAndDeletedFalse(activityId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", "id", activityId));

        if (request.getSubject() != null) activity.setSubject(request.getSubject());
        if (request.getDescription() != null) activity.setDescription(request.getDescription());
        if (request.getType() != null) activity.setType(request.getType());
        if (request.getStatus() != null) activity.setStatus(request.getStatus());
        if (request.getPriority() != null) activity.setPriority(request.getPriority());
        if (request.getDueDate() != null) activity.setDueDate(request.getDueDate());
        if (request.getStartTime() != null) activity.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) activity.setEndTime(request.getEndTime());
        if (request.getRelatedEntityType() != null) activity.setRelatedEntityType(request.getRelatedEntityType());
        if (request.getRelatedEntityId() != null) activity.setRelatedEntityId(request.getRelatedEntityId());
        if (request.getAssignedTo() != null) activity.setAssignedTo(request.getAssignedTo());
        if (request.getReminderAt() != null) {
            activity.setReminderAt(request.getReminderAt());
            activity.setReminderSent(false); // reset reminder on reschedule
        }
        if (request.getRecurrenceRule() != null) activity.setRecurrenceRule(request.getRecurrenceRule());
        if (request.getRecurrenceEnd() != null) activity.setRecurrenceEnd(request.getRecurrenceEnd());
        if (request.getLocation() != null) activity.setLocation(request.getLocation());
        if (request.getCallDurationMinutes() != null) activity.setCallDurationMinutes(request.getCallDurationMinutes());
        if (request.getCallOutcome() != null) activity.setCallOutcome(request.getCallOutcome());
        if (request.getEmailTo() != null) activity.setEmailTo(request.getEmailTo());
        if (request.getEmailCc() != null) activity.setEmailCc(request.getEmailCc());

        Activity updatedActivity = activityRepository.save(activity);
        log.info("Activity updated: {}", activityId);

        eventPublisher.publish("activity-events", tenantId, userId, "Activity",
                updatedActivity.getId().toString(), "ACTIVITY_UPDATED", activityMapper.toResponse(updatedActivity));

        return activityMapper.toResponse(updatedActivity);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "activities", key = "#activityId + '_' + T(com.crm.common.security.TenantContext).getTenantId()")
    public ActivityResponse getActivityById(UUID activityId) {
        String tenantId = TenantContext.getTenantId();
        log.debug("Fetching activity: {} for tenant: {}", activityId, tenantId);

        Activity activity = activityRepository.findByIdAndTenantIdAndDeletedFalse(activityId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", "id", activityId));

        return activityMapper.toResponse(activity);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityResponse> getAllActivities(int page, int size, String sortBy, String sortDir) {
        String tenantId = TenantContext.getTenantId();
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Activity> activityPage = activityRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);

        return buildPagedResponse(activityPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityResponse> getActivitiesByType(Activity.ActivityType type, int page, int size,
                                                                String sortBy, String sortDir) {
        String tenantId = TenantContext.getTenantId();
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Activity> activityPage = activityRepository.findByTenantIdAndTypeAndDeletedFalse(tenantId, type, pageable);

        return buildPagedResponse(activityPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityResponse> getActivitiesByStatus(Activity.ActivityStatus status, int page, int size,
                                                                  String sortBy, String sortDir) {
        String tenantId = TenantContext.getTenantId();
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Activity> activityPage = activityRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, status, pageable);

        return buildPagedResponse(activityPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityResponse> getActivitiesByTypeAndStatus(Activity.ActivityType type,
                                                                        Activity.ActivityStatus status,
                                                                        int page, int size,
                                                                        String sortBy, String sortDir) {
        String tenantId = TenantContext.getTenantId();
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Activity> activityPage = activityRepository.findByTenantIdAndTypeAndStatusAndDeletedFalse(
                tenantId, type, status, pageable);

        return buildPagedResponse(activityPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityResponse> getByRelatedEntity(String relatedEntityType, UUID relatedEntityId,
                                                               int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Activity> activityPage = activityRepository
                .findByTenantIdAndRelatedEntityTypeAndRelatedEntityIdAndDeletedFalse(
                        tenantId, relatedEntityType, relatedEntityId, pageable);

        return buildPagedResponse(activityPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityResponse> getByRelatedEntityId(UUID relatedEntityId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Activity> activityPage = activityRepository
                .findByTenantIdAndRelatedEntityIdAndDeletedFalse(tenantId, relatedEntityId, pageable);

        return buildPagedResponse(activityPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityResponse> getByAssignee(UUID assignedTo, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("dueDate").ascending());

        Page<Activity> activityPage = activityRepository
                .findByTenantIdAndAssignedToAndDeletedFalse(tenantId, assignedTo, pageable);

        return buildPagedResponse(activityPage);
    }

    @Transactional
    @CacheEvict(value = "activities", allEntries = true)
    public ActivityResponse markComplete(UUID activityId, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Marking activity as complete: {}", activityId);

        Activity activity = activityRepository.findByIdAndTenantIdAndDeletedFalse(activityId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", "id", activityId));

        activity.setStatus(Activity.ActivityStatus.COMPLETED);
        activity.setCompletedAt(LocalDateTime.now());

        Activity completedActivity = activityRepository.save(activity);

        eventPublisher.publish("activity-events", tenantId, userId, "Activity",
                completedActivity.getId().toString(), "ACTIVITY_COMPLETED", activityMapper.toResponse(completedActivity));

        // If this is a recurring task, spawn the next occurrence
        if (completedActivity.getRecurrenceRule() != null && completedActivity.getParentActivityId() == null) {
            spawnNextRecurrence(completedActivity);
        }

        return activityMapper.toResponse(completedActivity);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityResponse> getOverdueActivities(int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("dueDate").ascending());

        Page<Activity> activityPage = activityRepository.findOverdueActivities(tenantId, LocalDateTime.now(), pageable);

        return buildPagedResponse(activityPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityResponse> searchActivities(String query, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size);

        Page<Activity> activityPage = activityRepository.searchActivities(tenantId, query, pageable);

        return buildPagedResponse(activityPage);
    }

    @Transactional
    @CacheEvict(value = "activities", allEntries = true)
    public void deleteActivity(UUID activityId, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Soft deleting activity: {}", activityId);

        Activity activity = activityRepository.findByIdAndTenantIdAndDeletedFalse(activityId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", "id", activityId));

        activity.setDeleted(true);
        activityRepository.save(activity);

        eventPublisher.publish("activity-events", tenantId, userId, "Activity",
                activity.getId().toString(), "ACTIVITY_DELETED", null);
    }

    /* ============================================================
       Activity Timeline
       ============================================================ */

    @Transactional(readOnly = true)
    public List<ActivityResponse> getTimeline(UUID relatedEntityId) {
        String tenantId = TenantContext.getTenantId();
        return activityRepository
                .findByTenantIdAndRelatedEntityIdAndDeletedFalseOrderByCreatedAtDesc(tenantId, relatedEntityId)
                .stream().map(activityMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> getFullTimeline() {
        String tenantId = TenantContext.getTenantId();
        return activityRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId)
                .stream().map(activityMapper::toResponse).toList();
    }

    /* ============================================================
       Upcoming / Calendar
       ============================================================ */

    @Transactional(readOnly = true)
    public List<ActivityResponse> getUpcoming(int days) {
        String tenantId = TenantContext.getTenantId();
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.plusDays(days);
        return activityRepository.findUpcoming(tenantId, from, to)
                .stream().map(activityMapper::toResponse).toList();
    }

    /* ============================================================
       Analytics
       ============================================================ */

    @Transactional(readOnly = true)
    public ActivityAnalytics getAnalytics() {
        String tenantId = TenantContext.getTenantId();
        long total = activityRepository.countByTenantIdAndDeletedFalse(tenantId);
        long completed = activityRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, Activity.ActivityStatus.COMPLETED);
        long overdue = activityRepository.countOverdue(tenantId, LocalDateTime.now());
        Double avgDays = activityRepository.avgCompletionDays(tenantId);

        Map<String, Long> byType = new LinkedHashMap<>();
        for (Object[] row : activityRepository.countByType(tenantId)) {
            byType.put(row[0].toString(), (Long) row[1]);
        }

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : activityRepository.countByStatus(tenantId)) {
            byStatus.put(row[0].toString(), (Long) row[1]);
        }

        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Object[] row : activityRepository.countByPriority(tenantId)) {
            byPriority.put(row[0].toString(), (Long) row[1]);
        }

        Map<String, Long> byAssignee = new LinkedHashMap<>();
        for (Object[] row : activityRepository.countByAssignee(tenantId)) {
            byAssignee.put(row[0].toString(), (Long) row[1]);
        }

        return ActivityAnalytics.builder()
                .totalActivities(total)
                .completedActivities(completed)
                .overdueActivities(overdue)
                .completionRate(total > 0 ? (double) completed / total * 100.0 : 0)
                .avgCompletionDays(avgDays != null ? avgDays : 0)
                .countByType(byType)
                .countByStatus(byStatus)
                .countByPriority(byPriority)
                .countByAssignee(byAssignee)
                .build();
    }

    /* ============================================================
       Reminder Scheduler  (runs every minute)
       ============================================================ */

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void processReminders() {
        List<Activity> dueReminders = activityRepository.findDueReminders(LocalDateTime.now());
        for (Activity a : dueReminders) {
            log.info("Processing reminder for activity: {} ({})", a.getId(), a.getSubject());
            activityRepository.markReminderSent(a.getId());

            // Publish reminder event for the activity creator
            eventPublisher.publish("activity-events", a.getTenantId(),
                    a.getCreatedBy() != null ? a.getCreatedBy() : "system",
                    "Activity", a.getId().toString(), "ACTIVITY_REMINDER",
                    activityMapper.toResponse(a));

            // Also notify the assignee if different from the creator
            if (a.getAssignedTo() != null
                    && !a.getAssignedTo().toString().equals(a.getCreatedBy())) {
                eventPublisher.publish("activity-events", a.getTenantId(),
                        a.getAssignedTo().toString(),
                        "Activity", a.getId().toString(), "ACTIVITY_REMINDER",
                        activityMapper.toResponse(a));
            }
        }
        if (!dueReminders.isEmpty()) {
            log.info("Processed {} reminders", dueReminders.size());
        }
    }

    /* ============================================================
       Recurring Tasks  – spawn next occurrence when completed
       ============================================================ */

    private void spawnNextRecurrence(Activity completed) {
        LocalDateTime nextDue = calculateNextDue(completed.getDueDate(), completed.getRecurrenceRule());
        if (nextDue == null) return;
        if (completed.getRecurrenceEnd() != null && nextDue.toLocalDate().isAfter(completed.getRecurrenceEnd())) {
            log.info("Recurrence ended for activity: {}", completed.getId());
            return;
        }

        Activity next = Activity.builder()
                .type(completed.getType())
                .subject(completed.getSubject())
                .description(completed.getDescription())
                .priority(completed.getPriority())
                .dueDate(nextDue)
                .startTime(completed.getStartTime())
                .endTime(completed.getEndTime())
                .relatedEntityType(completed.getRelatedEntityType())
                .relatedEntityId(completed.getRelatedEntityId())
                .assignedTo(completed.getAssignedTo())
                .recurrenceRule(completed.getRecurrenceRule())
                .recurrenceEnd(completed.getRecurrenceEnd())
                .parentActivityId(completed.getId())
                .location(completed.getLocation())
                .build();
        next.setTenantId(completed.getTenantId());
        next.setStatus(Activity.ActivityStatus.NOT_STARTED);
        next.setPriority(completed.getPriority());

        // Auto-set reminder relative to new due date
        if (completed.getReminderAt() != null && completed.getDueDate() != null) {
            long reminderOffsetMinutes = java.time.Duration.between(completed.getReminderAt(), completed.getDueDate()).toMinutes();
            next.setReminderAt(nextDue.minusMinutes(reminderOffsetMinutes));
            next.setReminderSent(false);
        }

        activityRepository.save(next);
        log.info("Spawned recurring instance for activity: {} → new due: {}", completed.getId(), nextDue);
    }

    private LocalDateTime calculateNextDue(LocalDateTime current, Activity.RecurrenceRule rule) {
        if (current == null || rule == null) return null;
        return switch (rule) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case BIWEEKLY -> current.plusWeeks(2);
            case MONTHLY -> current.plusMonths(1);
        };
    }

    /* ============================================================
       Helpers
       ============================================================ */

    private PagedResponse<ActivityResponse> buildPagedResponse(Page<Activity> activityPage) {
        return PagedResponse.<ActivityResponse>builder()
                .content(activityPage.getContent().stream().map(activityMapper::toResponse).toList())
                .pageNumber(activityPage.getNumber())
                .pageSize(activityPage.getSize())
                .totalElements(activityPage.getTotalElements())
                .totalPages(activityPage.getTotalPages())
                .last(activityPage.isLast())
                .first(activityPage.isFirst())
                .build();
    }
}
