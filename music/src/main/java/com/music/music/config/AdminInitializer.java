package com.music.music.config;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.music.music.user.entity.Role;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@youaregabia.com}")
    private String adminEmail;

    @Value("${admin.password:Admin1234!}")
    private String adminPassword;

    @Value("${admin.name:관리자}")
    private String adminName;

    @Override
    public void run(String... args) {
        // 이미 admin 계정이 존재하면 생성 skip
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("[AdminInitializer] Admin 계정이 이미 존재합니다: {}", adminEmail);
            return;
        }

        User admin = User.builder()
                .name(adminName)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .phoneNumber("000-0000-0000")   // AES 암호화 필드 (더미값)
                .birthDate(LocalDate.of(1990, 1, 1))
                .ci("ADMIN_SYSTEM_CI")           // AES 암호화 필드, unique (고정값)
                .state(1)
                .build();

        userRepository.save(admin);
        log.info("[AdminInitializer] Admin 계정 생성 완료: {}", adminEmail);
    }
}
