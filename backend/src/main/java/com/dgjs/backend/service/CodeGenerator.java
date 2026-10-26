package com.dgjs.backend.service;

// 사번(E0001), 부서(D001), 프로젝트(P001) 같은 일련번호 코드를 만듦. 자릿수가 같아야 문자열 최댓값이 숫자 최댓값과 일치함
public final class CodeGenerator {

    private CodeGenerator() {
    }

    public static String next(String prefix, int width, String currentMax) {
        long current = 0;
        if (currentMax != null && currentMax.startsWith(prefix)) {
            current = Long.parseLong(currentMax.substring(prefix.length()));
        }
        long next = current + 1;
        long limit = (long) Math.pow(10, width);
        if (next >= limit) {
            throw new IllegalStateException("코드 자릿수를 모두 사용했습니다. (" + prefix + ")");
        }
        return prefix + String.format("%0" + width + "d", next);
    }
}
