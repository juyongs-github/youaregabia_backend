package com.music.music.user.entity;

import com.music.music.board.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_point")
public class UserPoint extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private int totalPoint = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Grade grade = Grade.ENSEMBLE;

    public void addPoint(int amount) {
        this.totalPoint += amount;
        updateGrade();
    }

    // 선차감 - 포인트 부족 시 예외
    public void deductPoint(int amount) {
        if (this.totalPoint < amount) {
            throw new IllegalStateException("포인트가 부족합니다.");
        }
        this.totalPoint = Math.max(0, this.totalPoint - amount);  // 0 이하로 안 내려감
        updateGrade();
    }

    private void updateGrade() {
        if (this.totalPoint >= 200000) {
            this.grade = Grade.LEGEND;
        } else if (this.totalPoint >= 100000) {
            this.grade = Grade.MAESTRO;
        } else if (this.totalPoint >= 50000) {
            this.grade = Grade.SOLOIST;
        } else if(this.totalPoint >= 10000) {
            this.grade = Grade.SESSION;
        } else{
            this.grade = Grade.ENSEMBLE;
        }
    }
}
