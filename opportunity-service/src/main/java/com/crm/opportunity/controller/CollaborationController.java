package com.crm.opportunity.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.common.dto.PagedResponse;
import com.crm.common.security.UserPrincipal;
import com.crm.opportunity.dto.*;
import com.crm.opportunity.service.DealApprovalService;
import com.crm.opportunity.service.DealChatService;
import com.crm.opportunity.service.MentionService;
import com.crm.opportunity.service.RecordCommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/collaboration")
@RequiredArgsConstructor
@Tag(name = "Collaboration", description = "Real-time collaboration APIs")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
public class CollaborationController {

    private final DealChatService chatService;
    private final MentionService mentionService;
    private final DealApprovalService approvalService;
    private final RecordCommentService commentService;

    /* ═══════════════════ DEAL CHAT ═══════════════════ */

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody SendChatMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String userName = principal.getEmail();
        ChatMessageResponse response = chatService.sendMessage(request, principal.getUserId(), userName);
        return ResponseEntity.ok(ApiResponse.success(response, "Message sent"));
    }

    @GetMapping("/chat/{opportunityId}")
    public ResponseEntity<ApiResponse<PagedResponse<ChatMessageResponse>>> getChatMessages(
            @PathVariable UUID opportunityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getMessages(opportunityId, page, size)));
    }

    @GetMapping("/chat/{parentMessageId}/replies")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getReplies(
            @PathVariable UUID parentMessageId) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getReplies(parentMessageId)));
    }

    @PutMapping("/chat/{messageId}")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> editMessage(
            @PathVariable UUID messageId,
            @RequestBody String newContent,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                chatService.editMessage(messageId, newContent, principal.getUserId())));
    }

    @DeleteMapping("/chat/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal UserPrincipal principal) {
        chatService.deleteMessage(messageId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Message deleted"));
    }

    /* ═══════════════════ MENTIONS ═══════════════════ */

    @GetMapping("/mentions")
    public ResponseEntity<ApiResponse<PagedResponse<MentionResponse>>> getMyMentions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                mentionService.getMyMentions(principal.getUserId(), page, size)));
    }

    @GetMapping("/mentions/unread")
    public ResponseEntity<ApiResponse<PagedResponse<MentionResponse>>> getUnreadMentions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                mentionService.getUnreadMentions(principal.getUserId(), page, size)));
    }

    @GetMapping("/mentions/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadMentionCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                mentionService.getUnreadCount(principal.getUserId())));
    }

    @PatchMapping("/mentions/{mentionId}/read")
    public ResponseEntity<ApiResponse<Void>> markMentionAsRead(
            @PathVariable UUID mentionId) {
        mentionService.markAsRead(mentionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Marked as read"));
    }

    @PatchMapping("/mentions/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllMentionsAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        mentionService.markAllAsRead(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "All mentions marked as read"));
    }

    /* ═══════════════════ DEAL APPROVALS ═══════════════════ */

    @PostMapping("/approvals")
    public ResponseEntity<ApiResponse<DealApprovalResponse>> createApproval(
            @Valid @RequestBody CreateApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String userName = principal.getEmail();
        return ResponseEntity.ok(ApiResponse.success(
                approvalService.createApproval(request, principal.getUserId(), userName),
                "Approval request created"));
    }

    @PostMapping("/approvals/{approvalId}/decide")
    public ResponseEntity<ApiResponse<DealApprovalResponse>> decideApproval(
            @PathVariable UUID approvalId,
            @Valid @RequestBody ApprovalDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                approvalService.decide(approvalId, request, principal.getUserId())));
    }

    @GetMapping("/approvals/opportunity/{opportunityId}")
    public ResponseEntity<ApiResponse<PagedResponse<DealApprovalResponse>>> getApprovalsByDeal(
            @PathVariable UUID opportunityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                approvalService.getByOpportunity(opportunityId, page, size)));
    }

    @GetMapping("/approvals/pending")
    public ResponseEntity<ApiResponse<PagedResponse<DealApprovalResponse>>> getPendingApprovals(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                approvalService.getPendingForApprover(principal.getUserId(), page, size)));
    }

    @GetMapping("/approvals/my-approvals")
    public ResponseEntity<ApiResponse<PagedResponse<DealApprovalResponse>>> getMyApprovals(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                approvalService.getMyApprovals(principal.getUserId(), page, size)));
    }

    @GetMapping("/approvals/my-requests")
    public ResponseEntity<ApiResponse<List<DealApprovalResponse>>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                approvalService.getMyRequests(principal.getUserId())));
    }

    @GetMapping("/approvals/pending-count")
    public ResponseEntity<ApiResponse<Long>> getPendingCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                approvalService.getPendingCount(principal.getUserId())));
    }

    /* ═══════════════════ RECORD COMMENTS ═══════════════════ */

    @PostMapping("/comments")
    public ResponseEntity<ApiResponse<RecordCommentResponse>> addComment(
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String userName = principal.getEmail();
        return ResponseEntity.ok(ApiResponse.success(
                commentService.addComment(request, principal.getUserId(), userName),
                "Comment added"));
    }

    @GetMapping("/comments/{recordType}/{recordId}")
    public ResponseEntity<ApiResponse<PagedResponse<RecordCommentResponse>>> getComments(
            @PathVariable String recordType,
            @PathVariable UUID recordId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                commentService.getComments(recordType, recordId, page, size)));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<RecordCommentResponse>> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                commentService.updateComment(commentId, request, principal.getUserId())));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        commentService.deleteComment(commentId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Comment deleted"));
    }
}
