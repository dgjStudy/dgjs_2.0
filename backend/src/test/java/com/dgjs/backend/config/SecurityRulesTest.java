package com.dgjs.backend.config;

import com.dgjs.backend.controller.AuthController;
import com.dgjs.backend.controller.DatasetController;
import com.dgjs.backend.controller.DepartmentController;
import com.dgjs.backend.controller.EmployeeController;
import com.dgjs.backend.controller.ProjectController;
import com.dgjs.backend.domain.Department;
import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.repository.EmployeeRepository;
import com.dgjs.backend.service.AuthService;
import com.dgjs.backend.service.DatasetService;
import com.dgjs.backend.service.DepartmentService;
import com.dgjs.backend.service.EmployeePrincipal;
import com.dgjs.backend.service.EmployeeService;
import com.dgjs.backend.service.LoginFailedException;
import com.dgjs.backend.service.LoginLockedException;
import com.dgjs.backend.service.ProjectService;
import com.dgjs.backend.service.UploadFileStorage;
import com.dgjs.backend.service.UploadTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 접근 규칙(비로그인, 일반 사용자, 관리자)과 요청마다의 사용자 상태 확인을 보안 필터 체인 전체로 검증함. DB는 쓰지 않음
@WebMvcTest(controllers = {AuthController.class, EmployeeController.class, DepartmentController.class,
        ProjectController.class, DatasetController.class})
@Import({SecurityConfig.class, SecurityErrorResponder.class})
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeRepository employeeRepository;
    @MockBean
    private AuthService authService;
    @MockBean
    private EmployeeService employeeService;
    @MockBean
    private DepartmentService departmentService;
    @MockBean
    private ProjectService projectService;
    @MockBean
    private DatasetService datasetService;
    @MockBean
    private UploadTaskService uploadTaskService;
    @MockBean
    private UploadFileStorage uploadFileStorage;

    private Employee employee(String status, boolean mustChangePassword) {
        return Employee.builder()
                .empNo("E0002").email("user@dgjs.local").passwordHash("hash")
                .department(Department.builder().deptCode("D001").name("부서").build())
                .role("USER").status(status).mustChangePassword(mustChangePassword)
                .build();
    }

    private UsernamePasswordAuthenticationToken loggedIn(Employee employee) {
        EmployeePrincipal principal = EmployeePrincipal.from(employee);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    @Test
    void 비로그인은_보호된_API에서_401을_받는다() throws Exception {
        mockMvc.perform(get("/api/datasets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void 로그인_API는_비로그인도_호출할_수_있다() throws Exception {
        when(authService.authenticate("a@dgjs.local", "pw")).thenThrow(new LoginFailedException());

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@dgjs.local\",\"password\":\"pw\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    void 잠긴_계정의_로그인은_429를_받는다() throws Exception {
        when(authService.authenticate("a@dgjs.local", "pw")).thenThrow(new LoginLockedException());

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@dgjs.local\",\"password\":\"pw\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void 로그인한_일반_사용자는_데이터셋과_부서_프로젝트_조회를_할_수_있다() throws Exception {
        when(employeeRepository.findById("E0002")).thenReturn(Optional.of(employee("ACTIVE", false)));

        mockMvc.perform(get("/api/datasets").with(authentication(loggedIn(employee("ACTIVE", false)))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/departments").with(authentication(loggedIn(employee("ACTIVE", false)))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/projects").with(authentication(loggedIn(employee("ACTIVE", false)))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void 일반_사용자는_사원_관리와_부서_프로젝트_쓰기가_거부된다() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
        mockMvc.perform(post("/api/departments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"부서\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/projects/P001/members").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"empNo\":\"E0002\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 관리자는_사원_관리와_부서_프로젝트_쓰기를_할_수_있다() throws Exception {
        mockMvc.perform(get("/api/employees")).andExpect(status().isOk());
        mockMvc.perform(post("/api/departments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"부서\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void CSRF_토큰이_없는_쓰기_요청은_거부된다() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"부서\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("요청을 확인하지 못했습니다. 화면을 새로고침한 뒤 다시 시도하세요."));
    }

    @Test
    void Swagger_문서_경로는_로그인_없이_열린다() throws Exception {
        int status = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void 비활성으로_바뀐_사용자의_기존_세션은_다음_요청부터_거부된다() throws Exception {
        Employee inactive = employee("INACTIVE", false);
        when(employeeRepository.findById("E0002")).thenReturn(Optional.of(inactive));

        mockMvc.perform(get("/api/datasets").with(authentication(loggedIn(employee("ACTIVE", false)))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 삭제된_사용자의_기존_세션도_거부된다() throws Exception {
        when(employeeRepository.findById("E0002")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/datasets").with(authentication(loggedIn(employee("ACTIVE", false)))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 관리자에서_일반_사용자로_바뀌면_관리_API가_즉시_거부된다() throws Exception {
        Employee admin = employee("ACTIVE", false);
        admin.setRole("ADMIN");
        UsernamePasswordAuthenticationToken session = loggedIn(admin);
        when(employeeRepository.findById("E0002")).thenReturn(Optional.of(employee("ACTIVE", false)));

        mockMvc.perform(get("/api/employees").with(authentication(session)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 임시_비밀번호_상태에서는_비밀번호_변경_관련_요청만_허용한다() throws Exception {
        Employee mustChange = employee("ACTIVE", true);
        when(employeeRepository.findById("E0002")).thenReturn(Optional.of(mustChange));

        mockMvc.perform(get("/api/datasets").with(authentication(loggedIn(mustChange))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("비밀번호를 먼저 변경해야 합니다."));
        mockMvc.perform(get("/api/auth/me").with(authentication(loggedIn(mustChange))))
                .andExpect(status().isOk());
    }
}
