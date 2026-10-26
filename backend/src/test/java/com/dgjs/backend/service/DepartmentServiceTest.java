package com.dgjs.backend.service;

import com.dgjs.backend.domain.Department;
import com.dgjs.backend.dto.DepartmentDto;
import com.dgjs.backend.repository.DepartmentRepository;
import com.dgjs.backend.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DepartmentServiceTest {

    private final DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final DepartmentService service =
            new DepartmentService(departmentRepository, employeeRepository, TestFixtures.directExecutor());

    @Test
    void 첫_부서는_D001이고_다음은_최댓값에_1을_더한다() {
        when(departmentRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg());
        when(departmentRepository.findMaxCode()).thenReturn(Optional.empty());
        assertThat(service.create(new DepartmentDto.Request(" 개발1팀 ")).getDeptCode()).isEqualTo("D001");

        when(departmentRepository.findMaxCode()).thenReturn(Optional.of("D007"));
        assertThat(service.create(new DepartmentDto.Request("개발2팀")).getDeptCode()).isEqualTo("D008");
    }

    @Test
    void 부서명이_비었거나_중복이면_거부한다() {
        when(departmentRepository.existsByName("개발팀")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new DepartmentDto.Request("  "))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(new DepartmentDto.Request("개발팀"))).isInstanceOf(IllegalArgumentException.class);
        verify(departmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void 소속_사원이_있는_부서는_삭제할_수_없다() {
        Department department = TestFixtures.department("D001", "개발팀");
        when(departmentRepository.findById("D001")).thenReturn(Optional.of(department));
        when(employeeRepository.existsByDepartment_DeptCode("D001")).thenReturn(true);

        assertThatThrownBy(() -> service.delete("D001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("소속 사원");
        verify(departmentRepository, never()).delete(any());
    }

    @Test
    void 소속_사원이_없으면_삭제한다() {
        Department department = TestFixtures.department("D002", "빈 부서");
        when(departmentRepository.findById("D002")).thenReturn(Optional.of(department));

        service.delete("D002");

        verify(departmentRepository).delete(department);
    }

    @Test
    void 목록은_부서별_소속_사원_수를_함께_내려준다() {
        when(departmentRepository.findAllByOrderByDeptCodeAsc()).thenReturn(List.of(
                TestFixtures.department("D001", "개발팀"), TestFixtures.department("D002", "사업팀")));
        when(employeeRepository.countGroupByDeptCode()).thenReturn(List.of(TestFixtures.codeCount("D001", 3)));

        List<DepartmentDto.Response> result = service.getAll();

        assertThat(result).extracting(DepartmentDto.Response::getEmployeeCount).containsExactly(3L, 0L);
    }

    @Test
    void 없는_부서는_찾을_수_없다() {
        when(departmentRepository.findById("D404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("D404", new DepartmentDto.Request("이름")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete("D404")).isInstanceOf(ResourceNotFoundException.class);
    }
}
