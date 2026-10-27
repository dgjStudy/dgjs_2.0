package com.dgjs.backend.service;

// 이메일 없음, 비밀번호 틀림, 비활성 계정을 구분하지 않고 같은 문구로 알림
public class LoginFailedException extends RuntimeException {

    public LoginFailedException() {
        super("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
