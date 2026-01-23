package com.music.music.common.dto;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.Builder;
import lombok.Data;

@Data
public class PageResultDTO<E> {

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

        // [1 2 3 4 5 6 7 8 9 10]
        // [11 12 ~~ 20]

        // page : 1 ~ 10 (start=1, end=10)
        // page=1&size=10
        int end = (int) (Math.ceil(pageRequestDTO.getPage() / 10.0)) * 10;
        int start = end - 9;

        // 실제 마지막 페이지 구하기
        int last = (int) (Math.ceil(totalCount / (double) pageRequestDTO.getSize()));

        // 10 > 5
        end = end > last ? last : end;

        // ---------- start, end 결정

        // 1 > 1 (이전 페이지 없음) / 11 > 1 (이전 페이지 있음)
        this.prev = start > 1;
        // 다음 페이지 여부
        this.next = totalCount > end * pageRequestDTO.getSize();

        // 이전페이지 번호
        if (prev) {
            this.prevPage = start - 1;
        }
        // 다음페이지 번호
        if (next) {
            this.nextPage = end + 1;
        }

        // IntStream.rangeClosed(start, end) : int 값 1,2,3,4 ~~ 10 => Integer
        // [1,2,3,4,5,6...]
        this.pageNumList = IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());
        totalPage = this.pageNumList.size();

        // 현재 사용자가 선택한 페이지
        this.current = pageRequestDTO.getPage();

    }

}
