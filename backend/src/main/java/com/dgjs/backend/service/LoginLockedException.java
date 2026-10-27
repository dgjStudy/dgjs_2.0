package com.dgjs.backend.service;

public class LoginLockedException extends RuntimeException {

    public LoginLockedException() {
        super("로그인 시도가 너무 많습니다. 15분 뒤에 다시 시도하세요.");
    }
}
