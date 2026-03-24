package com.music.music.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.common.AesUtil;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * 개인정보 암호화 마이그레이션 - 1회성 실행 엔드포인트
 * 실행 후 제거하거나 접근 제한 권장
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/migrate")
public class DataMigrationController {

    private final UserRepository userRepository;
    private final AesUtil aesUtil;

    @PersistenceContext
    private EntityManager em;

    @PostMapping("/encrypt-personal-info")
    @Transactional
    public ResponseEntity<String> encryptPersonalInfo() {
        List<User> users = userRepository.findAll();
        int count = 0;

        for (User user : users) {
            boolean updated = false;

            // CI: 평문이면 암호화 (ENC: 접두사 없으면 평문)
            if (user.getCi() != null && !user.getCi().startsWith("ENC:")) {
                em.createNativeQuery(
                    "UPDATE users SET ci = :encrypted WHERE id = :id")
                    .setParameter("encrypted", aesUtil.encrypt(user.getCi()))
                    .setParameter("id", user.getId())
                    .executeUpdate();
                updated = true;
            }

            // 전화번호
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().startsWith("ENC:")) {
                em.createNativeQuery(
                    "UPDATE users SET phone_number = :encrypted WHERE id = :id")
                    .setParameter("encrypted", aesUtil.encrypt(user.getPhoneNumber()))
                    .setParameter("id", user.getId())
                    .executeUpdate();
                updated = true;
            }

            // 주소
            if (user.getAddress() != null && !user.getAddress().startsWith("ENC:")) {
                em.createNativeQuery(
                    "UPDATE users SET address = :encrypted WHERE id = :id")
                    .setParameter("encrypted", aesUtil.encrypt(user.getAddress()))
                    .setParameter("id", user.getId())
                    .executeUpdate();
                updated = true;
            }

            // 상세주소
            if (user.getAddressDetail() != null && !user.getAddressDetail().startsWith("ENC:")) {
                em.createNativeQuery(
                    "UPDATE users SET address_detail = :encrypted WHERE id = :id")
                    .setParameter("encrypted", aesUtil.encrypt(user.getAddressDetail()))
                    .setParameter("id", user.getId())
                    .executeUpdate();
                updated = true;
            }

            if (updated) count++;
        }

        return ResponseEntity.ok("마이그레이션 완료: " + count + "명 처리됨");
    }
}
