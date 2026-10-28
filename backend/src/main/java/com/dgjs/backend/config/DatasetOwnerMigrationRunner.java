package com.dgjs.backend.config;

import com.dgjs.backend.service.DatasetOwnerMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// 초기 관리자 시드(AdminSeedRunner) 다음에 실행해야 넘길 관리자가 존재함
@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class DatasetOwnerMigrationRunner implements ApplicationRunner {

    private final DatasetOwnerMigrationService migrationService;

    @Override
    public void run(ApplicationArguments args) {
        int updated = migrationService.reassignUnknownOwners();
        if (updated > 0) {
            log.info("소유자를 알 수 없는 데이터셋 {}건의 소유자를 초기 관리자로 바꿨습니다.", updated);
        }
    }
}
