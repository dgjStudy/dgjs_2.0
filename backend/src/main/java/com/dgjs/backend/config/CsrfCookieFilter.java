package com.dgjs.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// CSRF 토큰은 사용 시점에 만들어지므로 모든 응답에 쿠키(XSRF-TOKEN)가 실리도록 한 번 읽어 줌
// 빈으로 등록하면 서블릿 필터로 한 번 더 등록되어 보안 필터 체인 안에서 건너뛰므로 SecurityConfig에서만 생성함
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken();
        }
        chain.doFilter(request, response);
    }
}
