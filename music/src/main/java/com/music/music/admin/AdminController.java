package com.music.music.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.board.repository.BoardRepository;
import com.music.music.board.repository.ReplyRepository;
import com.music.music.board.service.BoardService;
import com.music.music.board.service.ReplyService;
import com.music.music.chatbot.dto.InquiryResponseDto;
import com.music.music.chatbot.entity.InquiryStatus;
import com.music.music.chatbot.service.InquiryService;
import com.music.music.goods.dto.OrderDto;
import com.music.music.goods.entity.OrderStatus;
import com.music.music.goods.service.OrderService;
import com.music.music.recommendation.service.VectorIndexScheduler;
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
  private final BoardService boardService;
  private final ReplyService replyService;
  private final OrderService orderService;
  private final UserPointRepository userPointRepository;
  private final UserPointService userPointService;
  private final PointHistoryRepository pointHistoryRepository;
  private final InquiryService inquiryService;
  private final VectorIndexScheduler vectorIndexScheduler;

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
            "게시글", b.getBoardId(), b.getUser().getName(), b.getUser().getEmail(),
            b.getTitle(), b.getCreatedAt())));

    replyRepository.findAll().stream()
        .filter(r -> !r.isDeleted())
        .forEach(r -> combined.add(new ActivityLogDto(
            "댓글", r.getReplyId(), r.getUser().getName(), r.getUser().getEmail(),
            r.getContent().length() > 30 ? r.getContent().substring(0, 30) + "..." : r.getContent(),
            r.getCreatedAt())));

    combined.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));
    return ResponseEntity.ok(combined.subList(0, Math.min(100, combined.size())));
  }

  // 게시글 삭제 (관리자)
  @DeleteMapping("/boards/{boardId}")
  public ResponseEntity<Void> deleteBoard(@PathVariable Long boardId) {
    boardService.adminDeleteBoard(boardId);
    return ResponseEntity.ok().build();
  }

  // 댓글 삭제 (관리자)
  @DeleteMapping("/replies/{replyId}")
  public ResponseEntity<Void> deleteReply(@PathVariable Long replyId) {
    replyService.adminDeleteReply(replyId);
    return ResponseEntity.ok().build();
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

  // 주문 삭제 (탈퇴 회원 내역 정리용)
  @DeleteMapping("/orders/{orderId}")
  public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
    orderService.deleteOrder(orderId);
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

  // 전체 문의 내역 조회
  @GetMapping("/inquiries")
  public ResponseEntity<List<InquiryResponseDto>> getAllInquiries() {
    return ResponseEntity.ok(inquiryService.getAllInquiries());
  }

  // 문의 상태 변경 (접수중 → 답변완료)
  @PatchMapping("/inquiries/{id}/status")
  public ResponseEntity<Void> updateInquiryStatus(
      @PathVariable Long id,
      @RequestBody Map<String, String> body) {
    inquiryService.updateStatus(id, InquiryStatus.valueOf(body.get("status")));
    return ResponseEntity.ok().build();
  }

  // 답변 등록/수정
  @PutMapping("/inquiries/{id}/answer")
  public ResponseEntity<InquiryResponseDto> saveAnswer(
      @PathVariable Long id,
      @RequestBody Map<String, String> body) {
    return ResponseEntity.ok(inquiryService.saveAnswer(id, body.get("answer")));
  }

  // 답변 삭제
  @DeleteMapping("/inquiries/{id}/answer")
  public ResponseEntity<InquiryResponseDto> deleteAnswer(@PathVariable Long id) {
    return ResponseEntity.ok(inquiryService.deleteAnswer(id));
  }

  // 문의 삭제
  @DeleteMapping("/inquiries/{id}")
  public ResponseEntity<Void> deleteInquiry(@PathVariable Long id) {
    inquiryService.deleteInquiry(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/vector/index/run")
  public ResponseEntity<Map<String, Object>> runVectorIndexing() {
    vectorIndexScheduler.indexNewSongs();
    return ResponseEntity.ok(Map.of(
        "status", "ok",
        "message", "벡터 증분 인덱싱을 실행했습니다."));
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
      Long targetId,
      String name,
      String email,
      String content,
      LocalDateTime createdAt) {}
}
