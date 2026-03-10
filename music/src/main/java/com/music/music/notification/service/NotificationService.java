package com.music.music.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.notification.entity.Notification;
import com.music.music.notification.repository.NotificationRepository;
import com.music.music.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

  private final NotificationRepository notificationRepository;

  // 알림 생성
  public void createNotification(User receiver, String message, Long boardId) {
    Notification notification = Notification.builder()
        .receiver(receiver)
        .message(message)
        .boardId(boardId)
        .build();
    notificationRepository.save(notification);
  }

  // 내 알림 목록 조회
  @Transactional(readOnly = true)
  public List<Notification> getMyNotifications(String email) {
    return notificationRepository.findByReceiver_EmailOrderByCreatedAtDesc(email);
  }

  // 읽지 않은 알림 수
  @Transactional(readOnly = true)
  public long countUnread(String email) {
    return notificationRepository.countByReceiver_EmailAndIsReadFalse(email);
  }

  // 단건 읽음 처리
  public void markAsRead(Long id) {
    notificationRepository.findById(id).ifPresent(Notification::markAsRead);
  }

  // 전체 읽음 처리
  public void markAllAsRead(String email) {
    notificationRepository.findByReceiver_EmailOrderByCreatedAtDesc(email)
        .forEach(Notification::markAsRead);
  }
}
