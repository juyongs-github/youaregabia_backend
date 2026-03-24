package com.music.music.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA가 DB 저장/조회 시 자동으로 암호화/복호화
 * - 저장: plaintext → AES 암호화
 * - 조회: AES 암호화값 → plaintext
 */
@Converter
public class AesEncryptConverter implements AttributeConverter<String, String> {

    private final AesUtil aesUtil;

    public AesEncryptConverter(AesUtil aesUtil) {
        this.aesUtil = aesUtil;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return aesUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return aesUtil.decrypt(dbData);
    }
}
