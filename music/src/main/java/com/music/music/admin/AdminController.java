package com.music.music.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.board.repository.BoardRepository;
import com.music.music.board.repository.ReplyRepository;
import com.music.music.goods.dto.OrderDto;
import com.music.music.goods.entity.OrderStatus;
import com.music.music.goods.service.OrderService;
import com.music.music.user.entity.PointType;
import com.music.music.user.entity.Role;
import com.music.music.user.entity.User;
import com.music.music.user.entity.UserPoint;
import com.music.music.user.repository.PointHistoryRepository;
import com.music.music.user.repository.UserPointRepository;
import com.music.music.user.repository.UserRepository;
import com.music.music.user.service.UserPointService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

  private final UserRepository userRepository;
  private final UserLoginLogRepository loginLogRepository;
  private final BoardRepository boardRepository;
  private final ReplyRepository replyRepository;
  private final OrderService orderService;
  private final UserPointRepository userPointRepository;
  private final UserPointService userPointService;
  private final PointHistoryRepository pointHistoryRepository;

  // 전체 유저 목록 조회
  @GetMapping("/users")
  public ResponseEntity<List<UserSummaryDto>> getAllUsers() {
    List<UserSummaryDto> users = userRepository.findAll()
        .stream()
        .map(u -> new UserSummaryDto(
            u.getId(), u.getName(), u.getEmail(),
            u.getPhoneNumber(), u.getRole().name(),
            u.getState(), u.getCreatedAt()))
        .toList();
    return ResponseEntity.ok(users);
  }

  // 권한 변경
  @PatchMapping("/users/{id}/role")
  public ResponseEntity<Void> changeRole(
      @PathVariable Long id,
      @RequestBody Map<String, String> body) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
    user.setRole(Role.valueOf(body.get("role")));
    userRepository.save(user);
    return ResponseEntity.ok().build();
  }

  // 접속 로그 조회
  @GetMapping("/logs/login")
  public ResponseEntity<List<UserLoginLog>> getLoginLogs() {
    return ResponseEntity.ok(loginLogRepository.findTop100ByOrderByLoginAtDesc());
  }

  // 활동 로그 조회 (게시글 + 댓글)
  @GetMapping("/logs/activity")
  public ResponseEntity<List<ActivityLogDto>> getActivityLogs() {
    List<ActivityLogDto> combined = new ArrayList<>();

    boardRepository.findAll().stream()
        .filter(b -> !b.isDeleted())
        .forEach(b -> combined.add(new ActivityLogDto(
            "게시글", b.getUser().getName(), b.getUser().getEmail(),
            b.getTitle(), b.getCreatedAt())));

    replyRepository.findAll().stream()
        .filter(r -> !r.isDeleted())
        .forEach(r -> combined.add(new ActivityLogDto(
            "댓글", r.getUser().getName(), r.getUser().getEmail(),
            r.getContent().length() > 30 ? r.getContent().substring(0, 30) + "..." : r.getContent(),
            r.getCreatedAt())));

    combined.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));
    return ResponseEntity.ok(combined.subList(0, Math.min(100, combined.size())));
  }

  // 전체 주문 조회
  @GetMapping("/orders")
  public ResponseEntity<List<OrderDto>> getAllOrders() {
    return ResponseEntity.ok(orderService.getAllOrders());
  }

  // 주문 상태 변경
  @PatchMapping("/orders/{orderId}/status")
  public ResponseEntity<Void> updateOrderStatus(
      @PathVariable Long orderId,
      @RequestBody Map<String, String> body) {
    orderService.updateOrderStatus(orderId, OrderStatus.valueOf(body.get("status")));
    return ResponseEntity.ok().build();
  }

  // 운송장 번호 등록
  @PatchMapping("/orders/{orderId}/tracking")
  public ResponseEntity<Void> updateTracking(
      @PathVariable Long orderId,
      @RequestBody Map<String, String> body) {
    orderService.updateTracking(orderId, body.get("carrierId"), body.get("trackingNumber"));
    return ResponseEntity.ok().build();
  }

  // 전체 유저 포인트 목록
  @GetMapping("/points")
  public ResponseEntity<List<UserPointSummaryDto>> getAllUserPoints() {
    List<UserPoint> points = userPointRepository.findAll();
    List<UserPointSummaryDto> result = points.stream()
        .map(p -> new UserPointSummaryDto(
            p.getUser().getId(),
            p.getUser().getName(),
            p.getUser().getEmail(),
            p.getTotalPoint(),
            p.getGrade().name()))
        .toList();
    return ResponseEntity.ok(result);
  }

  // 관리자 포인트 조정
  @PostMapping("/points/{userId}/adjust")
  public ResponseEntity<Void> adjustUserPoint(
      @PathVariable Long userId,
      @RequestBody Map<String, Object> body) {
    int amount = (int) body.get("amount");
    userPointService.adminAdjustPoint(userId, amount);
    return ResponseEntity.ok().build();
  }

  // 관리자 포인트 지급/차감 로그
  @GetMapping("/points/logs")
  public ResponseEntity<List<AdminPointLogDto>> getAdminPointLogs() {
    List<AdminPointLogDto> logs = pointHistoryRepository
        .findByPointTypeInOrderByCreatedAtDesc(List.of(PointType.ADMIN_GRANT, PointType.ADMIN_DEDUCT))
        .stream()
        .map(h -> new AdminPointLogDto(
            h.getId(),
            h.getUser().getName(),
            h.getUser().getEmail(),
            h.getPointType().name(),
            h.getPointType().getDescription(),
            h.getAmount(),
            h.getCreatedAt()))
        .toList();
    return ResponseEntity.ok(logs);
  }

  public record AdminPointLogDto(
      Long id,
      String name,
      String email,
      String pointType,
      String description,
      int amount,
      LocalDateTime createdAt) {}

  public record UserPointSummaryDto(
      Long userId,
      String name,
      String email,
      int totalPoint,
      String grade) {}

  public record UserSummaryDto(
      Long id,
      String name,
      String email,
      String phoneNumber,
      String role,
      Integer state,
      LocalDateTime createdAt) {}

  public record ActivityLogDto(
      String type,
      String name,
      String email,
      String content,
      LocalDateTime createdAt) {}
}
