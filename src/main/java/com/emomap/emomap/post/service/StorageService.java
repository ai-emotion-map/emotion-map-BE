package com.emomap.emomap.post.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
public class StorageService {

    @Value("${app.upload.root:/home/ec2-user/app/uploads}")
    private String uploadRoot;

    public String storeFile(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("빈 파일입니다.");
            }

            String ct  = file.getContentType();
            String name = file.getOriginalFilename();
            String ext = (name != null && name.lastIndexOf('.') >= 0)
                    ? name.substring(name.lastIndexOf('.') + 1).toLowerCase()
                    : "";

            boolean okByCt  = (ct != null && ct.startsWith("image/"));
            boolean okByExt = ext.matches("^(jpg|jpeg|png|gif|webp|bmp)$");
            if (!okByCt && !okByExt) {
                throw new IllegalArgumentException("이미지 아님. ct=" + ct + ", name=" + name);
            }

            String folder = LocalDate.now().toString(); // YYYY-MM-DD
            Path dir = Paths.get(uploadRoot).resolve(folder);
            Files.createDirectories(dir);

            String base = (name == null ? "unnamed" : name).replaceAll("[\\s/\\\\]+", "_");
            String safe = UUID.randomUUID() + "_" + base;
            Path dest = dir.resolve(safe);

            log.debug("이미지 저장 시도: ct={}, dest={}", ct, dest);

            try (var in = file.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }

            // 정적/컨트롤러 매핑 기준 URL
            return "/uploads/" + folder + "/" + safe;
        } catch (Exception e) {
            log.error("이미지 저장 실패: {}", e.toString(), e);
            throw new RuntimeException("이미지 저장 실패", e);
        }
    }

    public Resource loadAsResource(String dateFolder, String fileName) {
        try {
            Path filePath = Paths.get(uploadRoot).resolve(dateFolder).resolve(fileName).normalize().toAbsolutePath();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NoSuchFileException(filePath.toString());
            }
            return resource;
        } catch (MalformedURLException | NoSuchFileException e) {
            throw new RuntimeException("이미지 찾을 수 없음", e);
        } catch (Exception e) {
            throw new RuntimeException("이미지 로드 실패", e);
        }
    }
}
