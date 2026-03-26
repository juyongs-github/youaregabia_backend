package com.music.music.chatbot.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RuleBasedService {

    private static final Map<String[], String> RULES = Map.ofEntries(
        Map.entry(new String[]{"추천 기능", "추천 방법", "추천 사용"}, """
                음악 추천 기능 사용법:
                1. 상단 메뉴에서 '추천' 탭을 클릭하세요.
                2. 기분/분위기 추천 — 현재 기분을 선택하면 어울리는 음악을 추천해드려요.
                3. 블라인드 추천 — 장르와 분위기를 설정하면 랜덤으로 곡을 추천받을 수 있어요.
                4. 이상형 월드컵 — 음악 월드컵으로 나만의 취향 플레이리스트를 만들어보세요.
                """),
        Map.entry(new String[]{"자유게시판", "자유 게시판"}, """
                자유게시판 이용 방법:
                1. 상단 메뉴 '커뮤니티 → 자유게시판'을 클릭하세요.
                2. '글쓰기' 버튼으로 자유롭게 글을 작성할 수 있어요.
                3. 다른 사용자의 글에 댓글과 좋아요를 남길 수 있어요.
                4. 음악 관련 이야기, 정보, 일상 등 다양한 주제로 소통해보세요.
                """),
        Map.entry(new String[]{"크리틱", "평론"}, """
                뮤직 크리틱 이용 방법:
                1. '추천 → 뮤직 크리틱' 메뉴로 이동하세요.
                2. 음악에 대한 나만의 평론을 작성하고 공유할 수 있어요.
                3. 다른 사용자의 평론을 읽고 댓글로 의견을 나눠보세요.
                4. 좋은 평론에는 좋아요로 공감을 표현할 수 있어요.
                """),
        Map.entry(new String[]{"마이페이지", "내 정보", "프로필"}, """
                마이페이지 이용 방법:
                1. 우측 상단 프로필 아이콘을 클릭하면 마이페이지로 이동해요.
                2. 내 정보 수정, 비밀번호 변경이 가능해요.
                3. 내가 만든 플레이리스트와 좋아요한 항목을 확인할 수 있어요.
                4. 현재 포인트, 등급, 활동 내역도 한눈에 볼 수 있어요.
                """),
        Map.entry(new String[]{"주문", "주문 내역"}, """
                주문 내역 및 배송 조회:
                1. '마이페이지 → 주문 내역'에서 확인할 수 있어요.
                2. 주문 상태(결제 완료 / 배송 중 / 배송 완료)를 실시간으로 확인할 수 있어요.
                3. 배송 조회 버튼을 클릭하면 운송장 번호를 확인할 수 있어요.
                """),
        Map.entry(new String[]{"퀴즈", "미니게임", "게임"}, """
                게임 이용 방법:
                1. 상단 메뉴에서 '게임' 탭을 클릭하세요.
                2. 뮤직 퀴즈 — 멜로디를 듣고 곡 제목/아티스트를 맞히는 청음 퀴즈예요.
                3. 앨범 퀴즈 — 앨범 커버를 보고 곡을 맞히는 퀴즈예요.
                4. 카드 매칭 — 음악 관련 카드를 뒤집어 짝을 맞히는 게임이에요.
                5. 게임 참여 시 포인트를 획득할 수 있어요.
                """)
    );

    public String getResponse(String message) {
        String lowerMsg = message.toLowerCase();
        for (Map.Entry<String[], String> entry : RULES.entrySet()) {
            for (String keyword : entry.getKey()) {
                if (lowerMsg.contains(keyword)) {
                    return entry.getValue().trim();
                }
            }
        }
        return null;
    }
}
