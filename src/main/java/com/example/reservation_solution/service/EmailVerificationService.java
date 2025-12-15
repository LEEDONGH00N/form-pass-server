package com.example.reservation_solution.service;

import com.example.reservation_solution.global.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final MailService mailService;
    private final Map<String, VerificationInfo> storage = new ConcurrentHashMap<>();

    private static final int CODE_EXPIRATION_MINUTES = 5; // 인증번호 유효시간
    private static final int SIGNUP_WINDOW_MINUTES = 30;  // 인증 후 회원가입까지 허용 시간

    public void sendCode(String email) {
        String authCode = generateAuthCode();
        storage.put(email, new VerificationInfo(authCode, LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES), false));
        mailService.sendAuthEmail(email, authCode);
    }

    public boolean verifyCode(String email, String inputCode) {
        VerificationInfo info = storage.get(email);
        if (info == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(info.expiryTime)) {
            storage.remove(email);
            return false;
        }

        if (info.code.equals(inputCode)) {
            storage.put(email, new VerificationInfo(info.code, LocalDateTime.now().plusMinutes(SIGNUP_WINDOW_MINUTES), true));
            return true;
        }
        return false;
    }

    public boolean isEmailVerified(String email) {
        VerificationInfo info = storage.get(email);
        if (info != null && info.verified && LocalDateTime.now().isBefore(info.expiryTime)) {
            storage.remove(email);
            return true;
        }
        return false;
    }

    private String generateAuthCode() {
        SecureRandom random = new SecureRandom();
        int num = random.nextInt(900000) + 100000;
        return String.valueOf(num);
    }
    private record VerificationInfo(String code, LocalDateTime expiryTime, boolean verified) {}
}