package com.dgjs.backend.service;

import com.dgjs.backend.domain.EmployeeRole;
import com.dgjs.backend.domain.EmployeeStatus;
import com.dgjs.backend.repository.DatasetMetaRepository;
import com.dgjs.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 로그인 도입 전에 적재된 데이터셋의 소유자(user_admin 등)를 가장 앞선 활성 관리자에게 넘김. 여러 번 실행해도 결과가 같음
@Service
@RequiredArgsConstructor
public class DatasetOwnerMigrationService {

    private final EmployeeRepository employeeRepository;
    private final DatasetMetaRepository metaRepository;

    @Transactional
    public int reassignUnknownOwners() {
        return employeeRepository
                .findFirstByRoleAndStatusOrderByEmpNoAsc(EmployeeRole.ADMIN.name(), EmployeeStatus.ACTIVE.name())
                .map(admin -> metaRepository.reassignUnknownOwners(admin.getEmpNo()))
                .orElse(0);
    }
}
