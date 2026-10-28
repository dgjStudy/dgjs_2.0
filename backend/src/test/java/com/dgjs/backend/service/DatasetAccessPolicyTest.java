package com.dgjs.backend.service;

import com.dgjs.backend.domain.DatasetMeta;
import com.dgjs.backend.domain.Project;
import com.dgjs.backend.repository.ProjectMemberRepository;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// 수정·삭제·프로젝트 연결 권한 규칙을 사용자 유형별로 확인함
class DatasetAccessPolicyTest {

    private final ProjectMemberRepository memberRepository = mock(ProjectMemberRepository.class);
    private final DatasetAccessPolicy policy = new DatasetAccessPolicy(memberRepository);

    private final EmployeePrincipal admin = TestFixtures.principal("E0001", "ADMIN");
    private final EmployeePrincipal owner = TestFixtures.principal("E0002", "USER");
    private final EmployeePrincipal member = TestFixtures.principal("E0003", "USER");
    private final EmployeePrincipal outsider = TestFixtures.principal("E0004", "USER");

    private DatasetMeta datasetOf(String projectCode) {
        Project project = projectCode == null ? null : Project.builder().projectCode(projectCode).name("사업").build();
        return DatasetMeta.builder().id(1L).name("데이터").ownerId("E0002").project(project).build();
    }

    private void stubMember(String projectCode, String empNo) {
        when(memberRepository.existsByProject_ProjectCodeAndEmployee_EmpNo(projectCode, empNo)).thenReturn(true);
    }

    @Test
    void 관리자는_모든_데이터셋을_수정하고_관리할_수_있다() {
        assertThat(policy.canEdit(admin, datasetOf(null))).isTrue();
        assertThat(policy.canEdit(admin, datasetOf("P001"))).isTrue();
        assertThat(policy.canManage(admin, datasetOf("P001"))).isTrue();
    }

    @Test
    void 소유자는_자기_데이터셋을_수정하고_관리할_수_있다() {
        assertThat(policy.canEdit(owner, datasetOf(null))).isTrue();
        assertThat(policy.canManage(owner, datasetOf("P001"))).isTrue();
    }

    @Test
    void 프로젝트_참여자는_수정은_되지만_관리는_안_된다() {
        stubMember("P001", "E0003");

        assertThat(policy.canEdit(member, datasetOf("P001"))).isTrue();
        assertThat(policy.canManage(member, datasetOf("P001"))).isFalse();
    }

    @Test
    void 참여하지_않은_사용자는_수정도_관리도_안_된다() {
        stubMember("P001", "E0003");

        assertThat(policy.canEdit(outsider, datasetOf("P001"))).isFalse();
        assertThat(policy.canManage(outsider, datasetOf("P001"))).isFalse();
    }

    @Test
    void 다른_프로젝트의_참여자는_수정할_수_없다() {
        stubMember("P002", "E0003");

        assertThat(policy.canEdit(member, datasetOf("P001"))).isFalse();
    }

    @Test
    void 프로젝트_미지정_데이터셋은_소유자와_관리자만_수정할_수_있다() {
        stubMember("P001", "E0003");

        assertThat(policy.canEdit(member, datasetOf(null))).isFalse();
        assertThat(policy.canEdit(outsider, datasetOf(null))).isFalse();
    }

    @Test
    void 미리_조회한_참여_프로젝트로도_같은_결과를_낸다() {
        assertThat(policy.canEdit(member, datasetOf("P001"), Set.of("P001"))).isTrue();
        assertThat(policy.canEdit(member, datasetOf("P001"), Set.of("P002"))).isFalse();
        assertThat(policy.canEdit(member, datasetOf(null), Set.of("P001"))).isFalse();
        assertThat(policy.canEdit(owner, datasetOf(null), Set.of())).isTrue();
    }

    @Test
    void 관리자는_참여_프로젝트를_조회하지_않는다() {
        assertThat(policy.myProjectCodes(admin)).isEmpty();
    }

    @Test
    void 권한이_없으면_403용_예외를_던진다() {
        assertThatThrownBy(() -> policy.requireEdit(outsider, datasetOf("P001")))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("수정");
        assertThatThrownBy(() -> policy.requireManage(outsider, datasetOf("P001"), "삭제"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("삭제");
        assertThatCode(() -> policy.requireEdit(owner, datasetOf("P001"))).doesNotThrowAnyException();
    }

    @Test
    void 프로젝트_연결은_관리자_또는_참여자만_할_수_있다() {
        stubMember("P001", "E0003");

        assertThatCode(() -> policy.requireAttach(admin, "P001")).doesNotThrowAnyException();
        assertThatCode(() -> policy.requireAttach(member, "P001")).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.requireAttach(outsider, "P001"))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> policy.requireAttach(member, "P002"))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
