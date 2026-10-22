package com.dgjs.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// 업로드 파일을 byte[]로 힙에 들고 있지 않도록 임시 파일에 저장함. 사용한 쪽이 delete로 지워야 함.
@Slf4j
@Component
public class UploadFileStorage {

    public Path save(MultipartFile file) throws IOException {
        Path path = Files.createTempFile("dgjs-upload-", ".tmp");
        try {
            file.transferTo(path);
        } catch (IOException | RuntimeException e) {
            delete(path);
            throw e;
        }
        return path;
    }

    public void delete(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("임시 파일 삭제 실패: {}", path, e);
        }
    }
}
