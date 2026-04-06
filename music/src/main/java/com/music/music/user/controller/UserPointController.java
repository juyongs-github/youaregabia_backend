package com.music.music.user.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.board.common.dto.PageRequestDTO;
import com.music.music.board.common.dto.PageResultDTO;
import com.music.music.user.dto.PointHistoryDto;
import com.music.music.user.entity.PointHistory;
import com.music.music.user.entity.PointType;
import com.music.music.user.entity.UserPoint;
import com.music.music.user.service.UserPointService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class UserPointController {
    
    private final UserPointService userPointService;

    // 내 포인트 조회
    @GetMapping("/me")
    public Map<String, Object> getMyPoint(@AuthenticationPrincipal String email) {
        UserPoint point = userPointService.getPoint(email);
        return Map.of(
            "totalPoint", point.getTotalPoint(),
            "accumulatedPoint", point.getAccumulatedPoint(),  // 추가
            "grade", point.getGrade()
        );
    }

    // 포인트 내역 조회
    @GetMapping("/history")
    public PageResultDTO<PointHistoryDto> getHistory(
        @AuthenticationPrincipal String email,
        PageRequestDTO dto,
        @RequestParam(defaultValue = "ALL") String filter) {

        Page<PointHistory> page = userPointService.getPointHistory(email, dto, filter);

        List<PointHistoryDto> dtoList = page.getContent().stream()
            .map(PointHistoryDto::new)
            .toList();

        return PageResultDTO.<PointHistoryDto>withAll()
            .dtoList(dtoList)
            .totalCount(page.getTotalElements())
            .pageRequestDTO(dto)
            .build();
    }
    // 포인트 차감 (결제 연동용)
    @PostMapping("/deduct")
    public void deductPoint(
        @AuthenticationPrincipal String email,
        @RequestBody Map<String, Integer> body) {
        userPointService.deductPoint(email, body.get("amount"));
    }

    // 게임 포인트
    @PostMapping("/quiz")
    public void addQuizPoint(
        @AuthenticationPrincipal String email,
        @RequestBody Map<String, Object> body) {
        int amount = (int) body.get("amount");
        String quizType = (String) body.get("quizType");
    
        PointType pointType = switch (quizType) {
            case "MUSIC" -> PointType.MUSIC_QUIZ;
            case "ALBUM" -> PointType.ALBUM_QUIZ;
            case "CARD" -> PointType.CARD_QUIZ;
            default -> PointType.MUSIC_QUIZ;
        };

    userPointService.addQuizPoint(email, pointType, amount);  // 한 번에 지급
    
    }
}