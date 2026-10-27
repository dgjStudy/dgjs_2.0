package com.dgjs.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

// 보안 필터 단계의 오류는 컨트롤러 예외 처리기를 거치지 않으므로 같은 { "message" } 형식으로 직접 씀
@Component
public class SecurityErrorResponder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Map.of("message", message)));
    }
}
