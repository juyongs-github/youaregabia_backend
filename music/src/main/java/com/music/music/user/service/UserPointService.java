package com.music.music.user.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.board.common.dto.PageRequestDTO;
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

    // 포인트 지급
    public void addPoint(String email, PointType pointType) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        UserPoint userPoint = userPointRepository.findByUser_Email(email)
            .orElseGet(() -> userPointRepository.save(
                UserPoint.builder().user(user).build()
            ));

        userPoint.addPoint(pointType.getDefaultAmount());

        pointHistoryRepository.save(PointHistory.builder()
            .user(user)
            .pointType(pointType)
            .amount(pointType.getDefaultAmount())
            .build());
    }

    // 포인트 차감 (결제 연동용)
    public void deductPoint(String email, int amount) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        UserPoint userPoint = userPointRepository.findByUser_Email(email)
            .orElseThrow(() -> new IllegalStateException("포인트 정보가 없습니다."));

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
    public Page<PointHistory> getPointHistory(String email, PageRequestDTO dto) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

    Pageable pageable = PageRequest.of(dto.getPage() - 1, dto.getSize());
    return pointHistoryRepository.findByUser_IdOrderByCreatedAtDesc(user.getId(), pageable);
}

    // 퀴즈 전용 - 점수만큼 한 번에 지급
    public void addQuizPoint(String email, PointType pointType, int score) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

    UserPoint userPoint = userPointRepository.findByUser_Email(email)
        .orElseGet(() -> userPointRepository.save(
            UserPoint.builder().user(user).build()
        ));

    userPoint.addPoint(score);  // 점수 한 번에 지급

    pointHistoryRepository.save(PointHistory.builder()
        .user(user)
        .pointType(pointType)
        .amount(score)  // 실제 점수
        .build());
    }
}
