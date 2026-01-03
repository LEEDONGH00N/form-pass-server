package com.example.reservation_solution.domain;

public enum QuestionType {
    TEXT,
    CHECKBOX,
    RADIO,
    NAME,   // 이름 필드 (guestName과 자동 매핑)
    PHONE   // 전화번호 필드 (guestPhoneNumber와 자동 매핑)
}
