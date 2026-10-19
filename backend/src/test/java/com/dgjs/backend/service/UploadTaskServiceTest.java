package com.dgjs.backend.service;

import com.dgjs.backend.dto.SchemaPreviewDto;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UploadTaskServiceTest {

    private final UploadTaskService service = new UploadTaskService();

    private boolean exists(String taskId) {
        SchemaPreviewDto.TaskProgressResponse progress = service.getProgress(taskId);
        return !"존재하지 않는 작업 ID입니다.".equals(progress.getMessage());
    }

    @Test
    void 등록하지_않은_작업은_FAILED로_응답한다() {
        SchemaPreviewDto.TaskProgressResponse progress = service.getProgress("none");

        assertThat(progress.getStatus()).isEqualTo("FAILED");
        assertThat(exists("none")).isFalse();
    }

    @Test
    void 등록한_작업은_PROCESSING_상태로_조회된다() {
        service.initTask("t1");

        assertThat(service.getProgress("t1").getStatus()).isEqualTo("PROCESSING");
        assertThat(service.getProgress("t1").getProgress()).isZero();
    }

    @Test
    void 완료된_작업은_10분_이내에는_유지한다() {
        service.initTask("t1");
        service.completeTask("t1", 7L);

        service.removeExpiredTasks(Instant.now().plus(Duration.ofMinutes(9)));

        assertThat(service.getProgress("t1").getStatus()).isEqualTo("COMPLETED");
        assertThat(service.getProgress("t1").getDatasetId()).isEqualTo(7L);
    }

    @Test
    void 완료된_작업은_10분_뒤_제거한다() {
        service.initTask("t1");
        service.completeTask("t1", 7L);

        service.removeExpiredTasks(Instant.now().plus(Duration.ofMinutes(11)));

        assertThat(exists("t1")).isFalse();
    }

    @Test
    void 실패한_작업도_10분_뒤_제거한다() {
        service.initTask("t1");
        service.failTask("t1", "오류");

        service.removeExpiredTasks(Instant.now().plus(Duration.ofMinutes(9)));
        assertThat(service.getProgress("t1").getStatus()).isEqualTo("FAILED");
        assertThat(exists("t1")).isTrue();

        service.removeExpiredTasks(Instant.now().plus(Duration.ofMinutes(11)));
        assertThat(exists("t1")).isFalse();
    }

    @Test
    void 진행_중인_작업은_시간이_지나도_제거하지_않는다() {
        service.initTask("t1");

        service.removeExpiredTasks(Instant.now().plus(Duration.ofHours(5)));

        assertThat(service.getProgress("t1").getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    void 일부_작업만_만료되어도_나머지는_유지한다() {
        service.initTask("done");
        service.completeTask("done", 1L);
        service.initTask("running");

        service.removeExpiredTasks(Instant.now().plus(Duration.ofMinutes(11)));

        assertThat(exists("done")).isFalse();
        assertThat(exists("running")).isTrue();
    }
}
