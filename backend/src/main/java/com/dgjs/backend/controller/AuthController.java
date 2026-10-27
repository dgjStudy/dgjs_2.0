package com.dgjs.backend.controller;

import com.dgjs.backend.dto.AuthDto;
import com.dgjs.backend.dto.EmployeeDto;
import com.dgjs.backend.service.AuthService;
import com.dgjs.backend.service.EmployeePrincipal;
import com.dgjs.backend.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmployeeService employeeService;
    private final SecurityContextRepository securityContextRepository;

    @Operation(summary = "로그인 (성공 시 세션 쿠키 발급, 5회 연속 실패하면 15분 잠금)")
    @PostMapping("/login")
    public ResponseEntity<EmployeeDto.Response> login(
            @RequestBody AuthDto.LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        Authentication authentication = authService.authenticate(body.getEmail(), body.getPassword());

        // 로그인 전 세션 ID가 로그인 후에도 쓰이지 않게 교체함
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(employeeService.get(principal.getEmpNo()));
    }

    @Operation(summary = "로그아웃 (세션 삭제)")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "현재 로그인한 사용자 조회")
    @GetMapping("/me")
    public ResponseEntity<EmployeeDto.Response> me(@AuthenticationPrincipal EmployeePrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(employeeService.get(principal.getEmpNo()));
    }

    @Operation(summary = "비밀번호 변경 (성공하면 임시 비밀번호 상태가 해제됨)")
    @PostMapping("/change-password")
    public ResponseEntity<EmployeeDto.Response> changePassword(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @RequestBody AuthDto.ChangePasswordRequest body) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.changePassword(principal.getEmpNo(), body.getCurrentPassword(), body.getNewPassword());
        return ResponseEntity.ok(employeeService.get(principal.getEmpNo()));
    }
}
