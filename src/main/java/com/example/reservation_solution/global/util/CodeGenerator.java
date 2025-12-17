package com.example.reservation_solution.global.util;

import java.security.SecureRandom;

public class CodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 10;
    private static final SecureRandom random = new SecureRandom();

    private CodeGenerator() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * 10자리 랜덤 이벤트 코드를 생성합니다.
     * 영문 대소문자(A-Z, a-z)와 숫자(0-9)로 구성됩니다.
     *
     * @return 생성된 10자리 코드
     */
    public static String generateEventCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }

        return code.toString();
    }
}
