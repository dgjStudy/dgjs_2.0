package com.dgjs.backend.config;

import com.dgjs.backend.service.ForbiddenOperationException;
import com.dgjs.backend.service.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonParseException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private void assertResponse(ResponseEntity<Map<String, String>> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).containsOnlyKeys("message").containsEntry("message", message);
    }

    @Test
    void 대상_없음은_404로_응답한다() {
        assertResponse(handler.handleNotFound(new ResourceNotFoundException("데이터셋을 찾을 수 없습니다.")),
                HttpStatus.NOT_FOUND, "데이터셋을 찾을 수 없습니다.");
    }

    @Test
    void 권한_없음은_403으로_응답한다() {
        assertResponse(handler.handleForbidden(new ForbiddenOperationException("수정할 권한이 없습니다.")),
                HttpStatus.FORBIDDEN, "수정할 권한이 없습니다.");
    }

    @Test
    void 검증_실패는_400으로_예외_메시지를_그대로_응답한다() {
        assertResponse(handler.handleBadRequest(new IllegalArgumentException("컬럼명이 비어 있습니다.")),
                HttpStatus.BAD_REQUEST, "컬럼명이 비어 있습니다.");
    }

    @Test
    void JSON_형식_오류는_400으로_응답한다() {
        assertResponse(handler.handleUnreadable(new JsonParseException(null, "Unexpected character")),
                HttpStatus.BAD_REQUEST, "요청 데이터 형식이 올바르지 않습니다.");
    }

    @Test
    void 요청_값_형식_오류는_400으로_응답한다() {
        MethodArgumentTypeMismatchException e = new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null);

        assertResponse(handler.handleTypeMismatch(e), HttpStatus.BAD_REQUEST, "요청 값의 형식이 올바르지 않습니다.");
    }

    @Test
    void 없는_경로는_404로_응답한다() {
        assertResponse(handler.handleNoResource(new NoResourceFoundException(HttpMethod.GET, "/nothing")),
                HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다.");
    }

    @Test
    void 지원하지_않는_메서드는_405로_응답한다() {
        assertResponse(handler.handleMethodNotAllowed(new HttpRequestMethodNotSupportedException("PATCH")),
                HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다.");
    }

    @Test
    void 예상하지_못한_예외는_내부_정보_없이_500으로_응답한다() {
        ResponseEntity<Map<String, String>> response =
                handler.handleUnexpected(new RuntimeException("select * from dataset_row where password = 'secret'"));

        assertResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
        assertThat(response.getBody().get("message")).doesNotContain("select", "secret");
    }
}
