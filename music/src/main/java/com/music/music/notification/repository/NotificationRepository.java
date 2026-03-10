package com.music.music.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findByReceiver_EmailOrderByCreatedAtDesc(String email);

  long countByReceiver_EmailAndIsReadFalse(String email);
}
