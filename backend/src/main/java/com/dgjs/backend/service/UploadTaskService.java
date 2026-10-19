package com.dgjs.backend.service;

import com.dgjs.backend.dto.SchemaPreviewDto;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UploadTaskService {

    // 종료된 작업을 곧바로 지우면 프론트의 마지막 진행률 조회가 실패하므로 일정 시간 보관함
    private static final Duration FINISHED_TASK_TTL = Duration.ofMinutes(10);

    private final Map<String, SchemaPreviewDto.TaskProgressResponse> taskMap = new ConcurrentHashMap<>();
    private final Map<String, Instant> finishedAtMap = new ConcurrentHashMap<>();

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
            finishedAtMap.put(taskId, Instant.now());
        }
    }

    public void failTask(String taskId, String errorMessage) {
        SchemaPreviewDto.TaskProgressResponse status = taskMap.get(taskId);
        if (status != null) {
            status.setStatus("FAILED");
            status.setMessage("적재 실패: " + errorMessage);
            finishedAtMap.put(taskId, Instant.now());
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

    @Scheduled(fixedDelay = 60_000)
    public void removeExpiredTasks() {
        Instant threshold = Instant.now().minus(FINISHED_TASK_TTL);
        finishedAtMap.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(threshold)) {
                taskMap.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
