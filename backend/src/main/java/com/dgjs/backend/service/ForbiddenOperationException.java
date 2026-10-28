package com.dgjs.backend.service;

// 로그인은 했지만 해당 작업을 할 권한이 없음(403)
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
