package com.music.music.notification.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.notification.entity.Notification;
import com.music.music.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  // 내 알림 목록
  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> getMyNotifications(Authentication auth) {
    String email = auth.getName();
    List<Notification> notifications = notificationService.getMyNotifications(email);

    List<Map<String, Object>> result = notifications.stream().map(n -> Map.<String, Object>of(
        "id", n.getId(),
        "message", n.getMessage(),
        "boardId", n.getBoardId() != null ? n.getBoardId() : 0L,
        "isRead", n.isRead(),
        "createdAt", n.getCreatedAt().toString()
    )).toList();

    return ResponseEntity.ok(result);
  }

  // 읽지 않은 알림 수
  @GetMapping("/unread-count")
  public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication auth) {
    long count = notificationService.countUnread(auth.getName());
    return ResponseEntity.ok(Map.of("count", count));
  }

  // 단건 읽음 처리
  @PatchMapping("/{id}/read")
  public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
    notificationService.markAsRead(id);
    return ResponseEntity.ok().build();
  }

  // 전체 읽음 처리
  @PatchMapping("/read-all")
  public ResponseEntity<Void> markAllAsRead(Authentication auth) {
    notificationService.markAllAsRead(auth.getName());
    return ResponseEntity.ok().build();
  }
}
