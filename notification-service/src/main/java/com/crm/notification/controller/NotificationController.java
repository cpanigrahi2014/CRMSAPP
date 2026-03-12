package com.crm.notification.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.common.dto.PagedResponse;
import com.crm.common.security.UserPrincipal;
import com.crm.notification.dto.CreateNotificationRequest;
import com.crm.notification.dto.NotificationResponse;
import com.crm.notification.entity.Notification;
import com.crm.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management APIs")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Create a new notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> create(
            @Valid @RequestBody CreateNotificationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        NotificationResponse response = notificationService.create(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Notification created successfully"));
    }

    @PostMapping("/{notificationId}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Send a pending notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> send(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        NotificationResponse response = notificationService.send(notificationId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Notification sent"));
    }

    @PostMapping("/{notificationId}/resend")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Resend a failed notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> resend(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        NotificationResponse response = notificationService.resend(notificationId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Notification resent"));
    }

    @GetMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(
            @PathVariable UUID notificationId) {
        NotificationResponse response = notificationService.getById(notificationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all notifications with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<NotificationResponse> response = notificationService.getAll(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/recipient/{recipient}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get notifications by recipient")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getByRecipient(
            @PathVariable String recipient,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<NotificationResponse> response = notificationService.getByRecipient(recipient, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get notifications by status")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getByStatus(
            @PathVariable Notification.NotificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<NotificationResponse> response = notificationService.getByStatus(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get notifications by type")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getByType(
            @PathVariable Notification.NotificationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<NotificationResponse> response = notificationService.getByType(type, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
