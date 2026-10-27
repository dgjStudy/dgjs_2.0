package com.dgjs.backend.service;

import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.domain.EmployeeStatus;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// 세션에 저장되는 로그인 사용자 정보임. 엔티티를 직접 들고 있지 않고 필요한 값만 복사함
public class EmployeePrincipal implements UserDetails, CredentialsContainer {

    @Getter
    private final String empNo;
    private final String email;
    private String passwordHash;
    @Getter
    private final String role;
    private final boolean active;
    @Getter
    private final boolean mustChangePassword;

    private EmployeePrincipal(Employee e) {
        this.empNo = e.getEmpNo();
        this.email = e.getEmail();
        this.passwordHash = e.getPasswordHash();
        this.role = e.getRole();
        this.active = EmployeeStatus.ACTIVE.name().equals(e.getStatus());
        this.mustChangePassword = e.isMustChangePassword();
    }

    public static EmployeePrincipal from(Employee employee) {
        return new EmployeePrincipal(employee);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }
}
