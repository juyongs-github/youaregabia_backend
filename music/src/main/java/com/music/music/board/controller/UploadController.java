package com.music.music.board.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${file.upload.path}")
    private String uploadPath;

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        String original = file.getOriginalFilename();
        String ext = original.substring(original.lastIndexOf("."));
        String filename = UUID.randomUUID() + ext;

        File dir = new File(uploadPath);
        if (!dir.exists()) dir.mkdirs();

        file.transferTo(new File(uploadPath + "/" + filename));

        return ResponseEntity.ok(Map.of("url", "/uploads/" + filename));
    }
}