package com.music.music.repository;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.music.music.board.repository.UserRepository;
import com.music.music.user.entity.User;
import com.music.music.user.entity.constant.Role;

@Disabled
@SpringBootTest
public class UserRepositoryTest {
    
    @Autowired
    UserRepository userRepository;

    @Test
    void userCreate(){

        User user = User.builder()
        .email("test@test.com")
        .password("123")
        .nickname("기리기리선")
        .role(Role.USER)
        .build();
        userRepository.save(user);
    }
    
}
