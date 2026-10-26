package com.dgjs.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

// 일련번호를 최댓값 + 1로 만들 때 동시 요청과 겹쳐 키가 충돌하면 새 트랜잭션에서 다시 시도함
@Component
@RequiredArgsConstructor
public class CodeRetryExecutor {

    private static final int MAX_ATTEMPTS = 3;

    private final TransactionTemplate transactionTemplate;

    public <T> T execute(Supplier<T> action) {
        DataIntegrityViolationException last = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return transactionTemplate.execute(status -> action.get());
            } catch (DataIntegrityViolationException e) {
                last = e;
            }
        }
        throw last;
    }
}
