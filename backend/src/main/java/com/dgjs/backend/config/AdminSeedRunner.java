package com.dgjs.backend.config;

import com.dgjs.backend.service.InitialSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeedRunner implements ApplicationRunner {

    private final InitialSeedService initialSeedService;

    @Value("${app.seed.dept-name:시스템관리}")
    private String deptName;

    @Value("${app.seed.admin-name:관리자}")
    private String adminName;

    @Value("${app.seed.admin-email:admin@dgjs.local}")
    private String adminEmail;

    // 비워 두면 임의로 생성해 로그에 한 번만 출력함
    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        initialSeedService.seedIfEmpty(deptName, adminName, adminEmail, adminPassword).ifPresent(result -> {
            if (result.generatedPassword() != null) {
                log.warn("초기 관리자를 생성했습니다. 사번={}, 이메일={}, 임시 비밀번호={} (다시 표시되지 않음)",
                        result.empNo(), result.email(), result.generatedPassword());
            } else {
                log.info("초기 관리자를 생성했습니다. 사번={}, 이메일={}", result.empNo(), result.email());
            }
        });
    }
}
