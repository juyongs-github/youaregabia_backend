package com.music.music.board.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.board.entity.ReplyLike;

public interface ReplyLikeRepository extends JpaRepository<ReplyLike, Long>{
     
}
