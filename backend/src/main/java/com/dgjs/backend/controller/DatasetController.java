package com.dgjs.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dgjs.backend.dto.DatasetDto;
import com.dgjs.backend.dto.SchemaPreviewDto;
import com.dgjs.backend.service.DatasetService;
import com.dgjs.backend.service.UploadFileStorage;
import com.dgjs.backend.service.UploadTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;
    private final UploadTaskService uploadTaskService;
    private final UploadFileStorage uploadFileStorage;
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
    public ResponseEntity<SchemaPreviewDto.Response> previewDataset(@RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(datasetService.previewAndValidate(file));
    }

    @PostMapping("/upload-async")
    public ResponseEntity<Map<String, Object>> uploadDatasetAsync(
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") String metadataJson) throws IOException {
        SchemaPreviewDto.FinalUploadRequest request = objectMapper.readValue(metadataJson, SchemaPreviewDto.FinalUploadRequest.class);
        datasetService.validateUploadRequest(request);
        String taskId = UUID.randomUUID().toString();

        // HTTP 요청 스레드가 종료되면 Tomcat 임시 파일이 삭제되므로, 비동기 처리용 임시 파일로 옮겨 경로만 넘김 (처리 후 비동기 쪽에서 삭제)
        Path uploadedFile = uploadFileStorage.save(file);
        String originalFilename = file.getOriginalFilename();

        try {
            // 비동기 스레드 시작 전에 첫 진행률 조회가 도착해도 작업이 존재해야 하므로 호출 전 등록임
            uploadTaskService.initTask(taskId);
            datasetService.processAsyncUpload(taskId, uploadedFile, originalFilename, request);
        } catch (RuntimeException e) {
            uploadFileStorage.delete(uploadedFile);
            throw e;
        }

        return ResponseEntity.ok(Map.of("success", true, "taskId", taskId, "message", "비동기 업로드가 시작되었습니다."));
    }

    @GetMapping("/tasks/{taskId}/progress")
    public ResponseEntity<SchemaPreviewDto.TaskProgressResponse> getTaskProgress(@PathVariable("taskId") String taskId) {
        return ResponseEntity.ok(uploadTaskService.getProgress(taskId));
    }

    @PostMapping("/datasets/manual")
    public ResponseEntity<Map<String, Object>> createManualDataset(@RequestBody DatasetDto.ManualCreateRequest request) {
        Long datasetId = datasetService.createManualDataset(request);
        return ResponseEntity.ok(Map.of("success", true, "datasetId", datasetId, "message", "데이터셋 스키마가 성공적으로 생성되었습니다."));
    }

    @PutMapping("/datasets/{id}")
    public ResponseEntity<Map<String, Object>> updateDatasetMeta(@PathVariable("id") Long id, @RequestBody DatasetDto.MetaUpdateRequest request) {
        datasetService.updateDatasetMeta(id, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "데이터셋 정보가 수정되었습니다."));
    }

    @DeleteMapping("/datasets/{id}")
    public ResponseEntity<Map<String, Object>> deleteDataset(@PathVariable("id") Long id) {
        datasetService.deleteDataset(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "데이터셋이 삭제되었습니다."));
    }

    @PostMapping("/datasets/{id}/rows")
    public ResponseEntity<Map<String, Object>> addRow(@PathVariable("id") Long id, @RequestBody Map<String, Object> rowData) throws JsonProcessingException {
        Long rowId = datasetService.addRow(id, rowData);
        return ResponseEntity.ok(Map.of("success", true, "rowId", rowId, "message", "데이터 행이 성공적으로 추가되었습니다."));
    }

    @PutMapping("/datasets/{id}/rows/{rowId}")
    public ResponseEntity<Map<String, Object>> updateRow(
            @PathVariable("id") Long id,
            @PathVariable("rowId") Long rowId,
            @RequestBody Map<String, Object> rowData) throws JsonProcessingException {
        datasetService.updateRow(id, rowId, rowData);
        return ResponseEntity.ok(Map.of("success", true, "message", "데이터 행이 수정되었습니다."));
    }

    @DeleteMapping("/datasets/{id}/rows/{rowId}")
    public ResponseEntity<Map<String, Object>> deleteRow(@PathVariable("id") Long id, @PathVariable("rowId") Long rowId) {
        datasetService.deleteRow(id, rowId);
        return ResponseEntity.ok(Map.of("success", true, "message", "데이터 행이 삭제되었습니다."));
    }
}
