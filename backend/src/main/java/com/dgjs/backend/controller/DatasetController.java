package com.dgjs.backend.controller;

import com.dgjs.backend.dto.DatasetDto;
import com.dgjs.backend.service.DatasetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;

    @GetMapping("/datasets")
    public ResponseEntity<List<DatasetDto.SummaryResponse>> getDatasets() {
        return ResponseEntity.ok(datasetService.getAllDatasets());
    }

    @GetMapping("/datasets/{id}")
    public ResponseEntity<DatasetDto.DetailResponse> getDatasetDetail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(datasetService.getDatasetDetail(id));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDataset(
            @RequestPart("file") MultipartFile file,
            @RequestPart("name") String name,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "category", required = false) String category,
            @RequestPart(value = "ownerId", required = false) String ownerId,
            @RequestPart(value = "orgId", required = false) String orgId) {
        try {
            DatasetDto.UploadRequest request = new DatasetDto.UploadRequest(name, description, category, ownerId, orgId);
            Long datasetId = datasetService.uploadDataset(file, request);
            return ResponseEntity.ok(Map.of("success", true, "datasetId", datasetId, "message", "데이터셋 및 스키마가 성공적으로 적재되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/datasets/{id}")
    public ResponseEntity<?> deleteDataset(@PathVariable("id") Long id) {
        try {
            datasetService.deleteDataset(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "데이터셋이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
