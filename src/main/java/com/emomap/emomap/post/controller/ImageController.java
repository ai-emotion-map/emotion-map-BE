package com.emomap.emomap.post.controller;

import com.emomap.emomap.post.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequiredArgsConstructor
@RequestMapping("/uploads")
public class ImageController {

    private final StorageService storageService;

    @GetMapping("/{date}/{fileName:.+}")
    public ResponseEntity<Resource> getImage(
            @PathVariable("date") String dateFolder,
            @PathVariable("fileName") String fileName
    ) {
        Resource resource = storageService.loadAsResource(dateFolder, fileName);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            // 가능하면 실제 MIME 추론
            mediaType = MediaType.parseMediaType(
                    Files.probeContentType(Path.of(resource.getFile().getAbsolutePath()))
            );
        } catch (Exception ignored) {}

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
