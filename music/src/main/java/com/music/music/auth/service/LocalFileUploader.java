package com.music.music.auth.service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileUploader {

  @Value("${file.upload.path}")
  private String uploadPath;

  public String upload(MultipartFile file, String dir) {
    System.out.println("🔥 uploadPath = " + uploadPath);

    try {
      // 디렉토리 생성
      String fullDirPath = uploadPath + "/" + dir;
      File directory = new File(fullDirPath);
      if (!directory.exists()) {
        directory.mkdirs();
      }

      // 파일명 생성
      String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
      File saveFile = new File(fullDirPath + "/" + filename);

      // 저장
      file.transferTo(saveFile);

      // 접근 가능한 URL 반환
      return "/uploads/" + dir + "/" + filename;

    } catch (IOException e) {
      throw new RuntimeException("파일 업로드 실패", e);
    }
  }
}
