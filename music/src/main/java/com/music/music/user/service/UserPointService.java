package com.music.music.user.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.board.common.dto.PageRequestDTO;
import com.music.music.ratelimit.RateLimiter;
import com.music.music.user.entity.PointHistory;
import com.music.music.user.entity.PointType;
import com.music.music.user.entity.User;
import com.music.music.user.entity.UserPoint;
import com.music.music.user.repository.PointHistoryRepository;
import com.music.music.user.repository.UserPointRepository;
import com.music.music.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserPointService {
    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;
    private final RateLimiter rateLimiter;

    // 포인트 지급
    public void addPoint(String email, PointType pointType) {

        
        User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));
        
        // 1. 배율 계산
        double multiplier = rateLimiter.getPointMultiplier(user.getEmail(), pointType);

        // 2. 배율이 0이면 적립 스킵
        if (multiplier == 0.0) {
            System.out.println(">>> PointDecay: 적립 스킵 (" + pointType + ")");
            return;
        }

        
        // 3. 배율 적용한 포인트 계산
        int basePoint = pointType.getDefaultAmount();
        int finalPoint = (int) Math.floor(basePoint * multiplier);
        
        // floor() 결과가 0점이면 히스토리 안 남김 (예: LIKE_GIVEN 1pt × 0.5 = 0)
        if (finalPoint <= 0) {
            System.out.println(">>> PointDecay: 0pt 스킵 (" + pointType + ")");
            return;
        }

        // 4. UserPoint 업데이트 — finalPoint 사용
        UserPoint userPoint = userPointRepository.findByUser_Email(email)
        .orElseGet(() -> userPointRepository.save(
            UserPoint.builder().user(user).build()
        ));

        userPoint.addPoint(finalPoint); 

        pointHistoryRepository.save(PointHistory.builder()
            .user(user)
            .pointType(pointType)
            .amount(finalPoint)
            .build());
    }

    // 포인트 차감 (결제 연동용)
    public void deductPoint(String email, int amount) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        UserPoint userPoint = userPointRepository.findByUser_Email(email)
            .orElseThrow(() -> new IllegalStateException("포인트 정보가 없습니다."));

            
        // 차감할 포인트가 없으면 스킵
        if (userPoint.getTotalPoint() <= 0) {
            System.out.println(">>> deductPoint 스킵: 잔여 포인트 없음 (" + email + ")");
            return;
        }

        userPoint.deductPoint(amount);

        pointHistoryRepository.save(PointHistory.builder()
            .user(user)
            .pointType(PointType.POINT_DEDUCT)
            .amount(-amount)
            .build());
    }

    // 포인트 조회
    @Transactional(readOnly = true)
    public UserPoint getPoint(String email) {
        return userPointRepository.findByUser_Email(email)
            .orElseGet(() -> UserPoint.builder()
                .user(userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다.")))
                .build());
    }

    // 포인트 내역 조회
    @Transactional(readOnly = true)
    public Page<PointHistory> getPointHistory(String email, PageRequestDTO dto, String filter) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        Pageable pageable = PageRequest.of(dto.getPage() - 1, dto.getSize());

        return switch (filter) {
            case "PLUS"  -> pointHistoryRepository
                .findByUser_IdAndAmountGreaterThanOrderByCreatedAtDesc(user.getId(), 0, pageable);
            case "MINUS" -> pointHistoryRepository
                .findByUser_IdAndAmountLessThanOrderByCreatedAtDesc(user.getId(), 0, pageable);
            default      -> pointHistoryRepository
                .findByUser_IdOrderByCreatedAtDesc(user.getId(), pageable);
        };
    }

    // 퀴즈 전용 - 점수만큼 한 번에 지급
    public void addQuizPoint(String email, PointType pointType, int score) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        // 1. 배율 계산 (MUSIC_QUIZ / ALBUM_QUIZ / CARD_QUIZ 각각 독립적으로 카운트)
        double multiplier = rateLimiter.getPointMultiplier(email, pointType);

        // 2. 배율이 0이면 적립 스킵
        if (multiplier == 0.0) {
            System.out.println(">>> QuizDecay: 적립 스킵 (" + pointType + ")");
            return;
        }

        // 3. score에 배율 적용
        int finalScore = (int) Math.floor(score * multiplier);
        System.out.println(">>> QuizDecay 적용: " + score + " × " + multiplier + " = " + finalScore);

        UserPoint userPoint = userPointRepository.findByUser_Email(email)
            .orElseGet(() -> userPointRepository.save(
                UserPoint.builder().user(user).build()
            ));

        userPoint.addPoint(finalScore);  // 점수 한 번에 지급

        pointHistoryRepository.save(PointHistory.builder()
            .user(user)
            .pointType(pointType)
            .amount(finalScore)  // 실제 점수
            .build());
    }

    // 좋아요 누를 때 — 누른 사람에게 LIKE_GIVEN 지급
    public void addLikeGivenPoint(String giverEmail) {
        addPoint(giverEmail, PointType.LIKE_GIVEN);
    }

    // 관리자 포인트 조정 (양수=지급, 음수=차감)
    public void adminAdjustPoint(Long userId, int amount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        UserPoint userPoint = userPointRepository.findByUser_Id(userId)
            .orElseGet(() -> userPointRepository.save(
                UserPoint.builder().user(user).build()
            ));

        if (amount > 0) {
            userPoint.addPoint(amount);
            pointHistoryRepository.save(PointHistory.builder()
                .user(user)
                .pointType(PointType.ADMIN_GRANT)
                .amount(amount)
                .build());
        } else if (amount < 0) {
            userPoint.deductPoint(-amount);
            pointHistoryRepository.save(PointHistory.builder()
                .user(user)
                .pointType(PointType.ADMIN_DEDUCT)
                .amount(amount)
                .build());
        }
    }
}
