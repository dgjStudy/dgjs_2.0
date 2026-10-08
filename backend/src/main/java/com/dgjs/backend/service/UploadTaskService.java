package com.dgjs.backend.service;

import com.dgjs.backend.dto.SchemaPreviewDto;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UploadTaskService {

    private final Map<String, SchemaPreviewDto.TaskProgressResponse> taskMap = new ConcurrentHashMap<>();

    public void initTask(String taskId) {
        taskMap.put(taskId, SchemaPreviewDto.TaskProgressResponse.builder()
                .taskId(taskId)
                .status("PROCESSING")
                .progress(0)
                .message("업로드 및 데이터 적재를 준비 중입니다...")
                .build());
    }

    public void updateProgress(String taskId, int progress, String message) {
        SchemaPreviewDto.TaskProgressResponse status = taskMap.get(taskId);
        if (status != null) {
            status.setProgress(progress);
            status.setMessage(message);
        }
    }

    public void completeTask(String taskId, Long datasetId) {
        SchemaPreviewDto.TaskProgressResponse status = taskMap.get(taskId);
        if (status != null) {
            status.setStatus("COMPLETED");
            status.setProgress(100);
            status.setMessage("데이터 적재가 성공적으로 완료되었습니다.");
            status.setDatasetId(datasetId);
        }
    }

    public void failTask(String taskId, String errorMessage) {
        SchemaPreviewDto.TaskProgressResponse status = taskMap.get(taskId);
        if (status != null) {
            status.setStatus("FAILED");
            status.setMessage("적재 실패: " + errorMessage);
        }
    }

    public SchemaPreviewDto.TaskProgressResponse getProgress(String taskId) {
        return taskMap.getOrDefault(taskId, SchemaPreviewDto.TaskProgressResponse.builder()
                .taskId(taskId)
                .status("FAILED")
                .progress(0)
                .message("존재하지 않는 작업 ID입니다.")
                .build());
    }

    public void removeTask(String taskId) {
        taskMap.remove(taskId);
    }
}
