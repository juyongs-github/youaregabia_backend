package com.music.music.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256/ECB 암호화 유틸
 * - 동일 입력 → 동일 출력 (결정적 암호화) → DB WHERE 검색 가능
 * - 암호화된 값은 "ENC:" 접두사로 식별 (마이그레이션 전 평문과 구분)
 */
@Component
public class AesUtil {

    private static final String PREFIX = "ENC:";
    private SecretKeySpec secretKeySpec;

    public AesUtil(@Value("${aes.secret-key}") String rawKey) throws Exception {
        // rawKey를 SHA-256으로 해싱해 32바이트 키 생성
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(rawKey.getBytes(StandardCharsets.UTF_8));
        keyBytes = Arrays.copyOf(keyBytes, 32);
        this.secretKeySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String value) {
        if (value == null) return null;
        if (value.startsWith(PREFIX)) return value; // 이미 암호화된 값
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("암호화 실패", e);
        }
    }

    public String decrypt(String value) {
        if (value == null) return null;
        if (!value.startsWith(PREFIX)) return value; // 마이그레이션 전 평문은 그대로 반환
        try {
            String base64 = value.substring(PREFIX.length());
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(base64));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("복호화 실패", e);
        }
    }
}
