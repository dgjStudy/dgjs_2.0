package com.dgjs.backend.service;

import com.dgjs.backend.domain.Department;
import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.repository.DatasetMetaRepository;
import com.dgjs.backend.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetOwnerMigrationServiceTest {

    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final DatasetMetaRepository metaRepository = mock(DatasetMetaRepository.class);
    private final DatasetOwnerMigrationService service =
            new DatasetOwnerMigrationService(employeeRepository, metaRepository);

    @Test
    void 가장_앞선_활성_관리자에게_소유자를_알_수_없는_데이터셋을_넘긴다() {
        Employee admin = TestFixtures.employee("E0001", "ADMIN", "ACTIVE", Department.builder().deptCode("D001").name("부서").build());
        when(employeeRepository.findFirstByRoleAndStatusOrderByEmpNoAsc("ADMIN", "ACTIVE")).thenReturn(Optional.of(admin));
        when(metaRepository.reassignUnknownOwners("E0001")).thenReturn(4);

        assertThat(service.reassignUnknownOwners()).isEqualTo(4);
    }

    @Test
    void 활성_관리자가_없으면_아무것도_바꾸지_않는다() {
        when(employeeRepository.findFirstByRoleAndStatusOrderByEmpNoAsc("ADMIN", "ACTIVE")).thenReturn(Optional.empty());

        assertThat(service.reassignUnknownOwners()).isZero();
        verify(metaRepository, never()).reassignUnknownOwners(anyString());
    }
}
