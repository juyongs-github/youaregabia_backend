package com.music.music.common;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Builder;
import lombok.Data;

@Data
public class PageResultDTO<E> {
    // E는 변수임, 후일 넣는 타입에 맞춰서 바뀜 유연하게 활용
    // 화면에 보여줄 목록
    private List<E> dtoList;
    // 페이지 번호 목록
    private List<Integer> pageNumList;
    private PageRequestDTO pageRequestDTO;
    private boolean prev, next;
    private int prevPage, nextPage, totalPage, current;
    private long totalCount;

    @Builder(builderMethodName = "withAll")
    public PageResultDTO(List<E> dtoList, PageRequestDTO pageRequestDTO, long totalCount) {
        this.dtoList = dtoList;
        this.pageRequestDTO = pageRequestDTO;
        this.totalCount = totalCount;

        // 1~ 10, 11~20 등 하단 페이지번호
        // 자바는 나누면 몫만 가져온다. page가 3이라면 0.3 -> 올림하면 end=10, start=9
        // page 11이라면 올리면 2 end는 20, start는 19. 이 방법으로 페이지번호를 만든다.
        int end = (int) (Math.ceil(pageRequestDTO.getPage()/10.0)) * 10;
        int start = end - 9;
        
        // 실제 마지막 페이지 구하기-> 개수/전체개수
        int last = (int) (Math.ceil(totalCount/(double) pageRequestDTO.getSize()));
        
        // end가 last보다 커지면 실제 마지막페이지인 last로, 아니라면 end를 출력하는 것.
        end = end > last ? last : end;

        // 1>1 이전 페이지로 갈 게 없다. 11>1 이전페이지가 있다.
        this.prev = start > 1;
        //다음페이지 여부
        this.next = totalCount > end * pageRequestDTO.getSize();
        // 이전페이지 번호
        if (prev) {
            this.prevPage = start -1;
        }
        // 다음 페이지 번호
        if (next) {
            this.nextPage = end + 1;
        }
        // intstream~ : int 값 1~10 을 list로 모으고 박스드로 Inteager로 만들기-> [1,2,3,4,...]
        this.pageNumList = IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());
        // 길이
        totalPage = this.pageNumList.size();
        // 현재 사용자가 선택한 페이지
        this.current = pageRequestDTO.getPage();
    }
    
}
