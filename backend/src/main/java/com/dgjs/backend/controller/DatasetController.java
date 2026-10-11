package com.dgjs.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dgjs.backend.dto.DatasetDto;
import com.dgjs.backend.dto.SchemaPreviewDto;
import com.dgjs.backend.service.DatasetService;
import com.dgjs.backend.service.UploadTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;
    private final UploadTaskService uploadTaskService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/datasets")
    public ResponseEntity<List<DatasetDto.SummaryResponse>> getDatasets() {
        return ResponseEntity.ok(datasetService.getAllDatasets());
    }

    @GetMapping("/datasets/{id}")
    public ResponseEntity<DatasetDto.DetailResponse> getDatasetDetail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(datasetService.getDatasetDetail(id));
    }

    @GetMapping("/datasets/{id}/rows")
    public ResponseEntity<DatasetDto.PagedRowsResponse> getDatasetRows(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(datasetService.getDatasetRowsPaged(id, page, size));
    }

    @PostMapping("/preview")
    public ResponseEntity<?> previewDataset(@RequestPart("file") MultipartFile file) {
        try {
            SchemaPreviewDto.Response previewResponse = datasetService.previewAndValidate(file);
            return ResponseEntity.ok(previewResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "warnings", List.of(e.getMessage())));
        }
    }

    @PostMapping("/upload-async")
    public ResponseEntity<?> uploadDatasetAsync(
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") String metadataJson) {
        try {
            SchemaPreviewDto.FinalUploadRequest request = objectMapper.readValue(metadataJson, SchemaPreviewDto.FinalUploadRequest.class);
            String taskId = UUID.randomUUID().toString();
            
            // HTTP 요청 스레드가 종료되면서 Tomcat 임시 임시파일(upload_xxx.tmp)이 삭제되는 현상을 방지하기 위해 바이트 배열을 추출
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            
            datasetService.processAsyncUpload(taskId, fileBytes, originalFilename, request);
            
            return ResponseEntity.ok(Map.of("success", true, "taskId", taskId, "message", "비동기 업로드가 시작되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/tasks/{taskId}/progress")
    public ResponseEntity<SchemaPreviewDto.TaskProgressResponse> getTaskProgress(@PathVariable("taskId") String taskId) {
        return ResponseEntity.ok(uploadTaskService.getProgress(taskId));
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

    @PostMapping("/datasets/manual")
    public ResponseEntity<?> createManualDataset(@RequestBody DatasetDto.ManualCreateRequest request) {
        try {
            Long datasetId = datasetService.createManualDataset(request);
            return ResponseEntity.ok(Map.of("success", true, "datasetId", datasetId, "message", "데이터셋 스키마가 성공적으로 생성되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/datasets/{id}")
    public ResponseEntity<?> updateDatasetMeta(@PathVariable("id") Long id, @RequestBody DatasetDto.MetaUpdateRequest request) {
        try {
            datasetService.updateDatasetMeta(id, request);
            return ResponseEntity.ok(Map.of("success", true, "message", "데이터셋 정보가 수정되었습니다."));
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

    @PostMapping("/datasets/{id}/rows")
    public ResponseEntity<?> addRow(@PathVariable("id") Long id, @RequestBody Map<String, Object> rowData) {
        try {
            Long rowId = datasetService.addRow(id, rowData);
            return ResponseEntity.ok(Map.of("success", true, "rowId", rowId, "message", "데이터 행이 성공적으로 추가되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/datasets/{id}/rows/{rowId}")
    public ResponseEntity<?> updateRow(
            @PathVariable("id") Long id,
            @PathVariable("rowId") Long rowId,
            @RequestBody Map<String, Object> rowData) {
        try {
            datasetService.updateRow(id, rowId, rowData);
            return ResponseEntity.ok(Map.of("success", true, "message", "데이터 행이 수정되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/datasets/{id}/rows/{rowId}")
    public ResponseEntity<?> deleteRow(@PathVariable("id") Long id, @PathVariable("rowId") Long rowId) {
        try {
            datasetService.deleteRow(id, rowId);
            return ResponseEntity.ok(Map.of("success", true, "message", "데이터 행이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}

