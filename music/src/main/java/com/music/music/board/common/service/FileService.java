package com.music.music.board.common.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {
    @Value("${file.upload.dir}")
    private String uploadDir;

    public String upload(MultipartFile file) {
        validate(file);

        try {
            // 1. 업로드 루트 디렉터리
            Path rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();

            // 2. 디렉터리 없으면 생성
            Files.createDirectories(rootDir);

            // 3. 파일명 생성
            String fileOriginName = file.getOriginalFilename();
            String filename = System.currentTimeMillis() + "_" + fileOriginName;

            // 4. 저장 경로
            Path targetPath = rootDir.resolve(filename).normalize();

            // 5. 파일 저장
            file.transferTo(targetPath.toFile());

            // 6. URL 반환
            return "/uploads/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }

    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.contains("default")) {
            return; // 기본 이미지 삭제 방지
        }

        try {
            Path path = Paths.get(uploadDir, fileUrl.replace("/uploads/", ""));
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패", e);
        }
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 없습니다.");
        }

        if (!file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }
    }

    private String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
