package com.dgjs.backend.service;

import com.dgjs.backend.domain.DatasetMeta;
import com.dgjs.backend.domain.EmployeeRole;
import com.dgjs.backend.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

// 데이터셋 수정·삭제·프로젝트 연결 권한을 판정하는 유일한 곳임. 조회는 로그인한 모두에게 열려 있어 여기서 다루지 않음
//  - 관리(삭제, 프로젝트 연결 변경): ADMIN, 소유자
//  - 수정(메타, 행): 관리 권한자 + 그 데이터셋 프로젝트의 참여자
//  - 연결 대상 프로젝트: ADMIN 또는 해당 프로젝트 참여자
@Component
@RequiredArgsConstructor
public class DatasetAccessPolicy {

    private final ProjectMemberRepository projectMemberRepository;

    public boolean isAdmin(EmployeePrincipal user) {
        return EmployeeRole.ADMIN.name().equals(user.getRole());
    }

    // 목록처럼 여러 건을 판정할 때 참여 프로젝트를 한 번만 조회하려고 미리 받아 둠. ADMIN은 필요 없어 비워 둠
    public Set<String> myProjectCodes(EmployeePrincipal user) {
        return isAdmin(user) ? Set.of() : projectMemberRepository.findProjectCodesByEmpNo(user.getEmpNo());
    }

    public boolean canManage(EmployeePrincipal user, DatasetMeta meta) {
        return isAdmin(user) || user.getEmpNo().equals(meta.getOwnerId());
    }

    public boolean canEdit(EmployeePrincipal user, DatasetMeta meta, Set<String> myProjectCodes) {
        if (canManage(user, meta)) {
            return true;
        }
        String projectCode = projectCodeOf(meta);
        return projectCode != null && myProjectCodes.contains(projectCode);
    }

    public boolean canEdit(EmployeePrincipal user, DatasetMeta meta) {
        if (canManage(user, meta)) {
            return true;
        }
        String projectCode = projectCodeOf(meta);
        return projectCode != null
                && projectMemberRepository.existsByProject_ProjectCodeAndEmployee_EmpNo(projectCode, user.getEmpNo());
    }

    public void requireEdit(EmployeePrincipal user, DatasetMeta meta) {
        if (!canEdit(user, meta)) {
            throw new ForbiddenOperationException("이 데이터셋을 수정할 권한이 없습니다. (소유자, 프로젝트 참여자, 관리자만 가능)");
        }
    }

    public void requireManage(EmployeePrincipal user, DatasetMeta meta, String action) {
        if (!canManage(user, meta)) {
            throw new ForbiddenOperationException("이 데이터셋의 " + action + " 권한이 없습니다. (소유자, 관리자만 가능)");
        }
    }

    public void requireAttach(EmployeePrincipal user, String projectCode) {
        if (isAdmin(user)) {
            return;
        }
        if (!projectMemberRepository.existsByProject_ProjectCodeAndEmployee_EmpNo(projectCode, user.getEmpNo())) {
            throw new ForbiddenOperationException("참여 중인 프로젝트에만 데이터셋을 연결할 수 있습니다.");
        }
    }

    static String projectCodeOf(DatasetMeta meta) {
        return meta.getProject() == null ? null : meta.getProject().getProjectCode();
    }
}
