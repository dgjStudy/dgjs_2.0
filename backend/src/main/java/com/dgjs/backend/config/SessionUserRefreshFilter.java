package com.dgjs.backend.config;

import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.domain.EmployeeStatus;
import com.dgjs.backend.repository.EmployeeRepository;
import com.dgjs.backend.service.EmployeePrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

// 요청마다 로그인 사용자의 상태·역할을 다시 읽어 비활성 처리와 역할 변경이 즉시 반영되게 하고,
// 임시 비밀번호 상태에서는 비밀번호 변경 관련 요청만 허용함
// 빈으로 등록하면 서블릿 필터로 한 번 더 등록되어 보안 필터 체인 안에서 건너뛰므로 SecurityConfig에서만 생성함
@RequiredArgsConstructor
public class SessionUserRefreshFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_WHILE_PASSWORD_CHANGE_REQUIRED =
            Set.of("/api/auth/me", "/api/auth/logout", "/api/auth/change-password");

    private final EmployeeRepository employeeRepository;
    private final SecurityErrorResponder errorResponder;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof EmployeePrincipal principal) {
            Optional<Employee> latest = employeeRepository.findById(principal.getEmpNo());
            if (latest.isEmpty() || !EmployeeStatus.ACTIVE.name().equals(latest.get().getStatus())) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                SecurityContextHolder.clearContext();
            } else {
                EmployeePrincipal fresh = EmployeePrincipal.from(latest.get());
                SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(fresh, null, fresh.getAuthorities()));
                if (fresh.isMustChangePassword()
                        && !ALLOWED_WHILE_PASSWORD_CHANGE_REQUIRED.contains(request.getRequestURI())) {
                    errorResponder.write(response, HttpServletResponse.SC_FORBIDDEN, "비밀번호를 먼저 변경해야 합니다.");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
