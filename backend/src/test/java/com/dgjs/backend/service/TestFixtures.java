package com.dgjs.backend.service;

import com.dgjs.backend.domain.Department;
import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.repository.CodeCount;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// 사원·부서·프로젝트 서비스 테스트가 공유하는 생성 도우미임
final class TestFixtures {

    private TestFixtures() {
    }

    // 트랜잭션 없이 콜백을 그대로 실행하는 실행기
    static CodeRetryExecutor directExecutor() {
        TransactionTemplate template = mock(TransactionTemplate.class);
        when(template.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
        return new CodeRetryExecutor(template);
    }

    static Department department(String code, String name) {
        return Department.builder().deptCode(code).name(name).build();
    }

    static Employee employee(String empNo, String role, String status, Department department) {
        return Employee.builder()
                .empNo(empNo)
                .name("테스트")
                .email(empNo.toLowerCase() + "@test.local")
                .passwordHash("hash")
                .department(department)
                .jobTitle("사원")
                .role(role)
                .status(status)
                .build();
    }

    static CodeCount codeCount(String code, long cnt) {
        return new CodeCount() {
            @Override
            public String getCode() {
                return code;
            }

            @Override
            public long getCnt() {
                return cnt;
            }
        };
    }
}
