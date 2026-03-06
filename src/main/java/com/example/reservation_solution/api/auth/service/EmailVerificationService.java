package com.example.reservation_solution.api.auth.service;

import com.example.reservation_solution.api.auth.domain.VerificationInfo;
import com.example.reservation_solution.api.auth.repository.VerificationStore;
import com.example.reservation_solution.global.config.VerificationProperties;
import com.example.reservation_solution.global.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final MailService mailService;
    private final VerificationStore verificationStore;
    private final VerificationProperties verificationProperties;

    public void sendCode(String email) {
        String authCode = generateAuthCode();
        verificationStore.save(email, new VerificationInfo(authCode,
                LocalDateTime.now().plusMinutes(verificationProperties.getCodeExpirationMinutes()), false));
        mailService.sendAuthEmail(email, authCode);
    }

    public boolean verifyCode(String email, String inputCode) {
        VerificationInfo info = verificationStore.find(email);
        if (info == null) {
            return false;
        }
        if (info.code().equals(inputCode)) {
            verificationStore.save(email, new VerificationInfo(info.code(),
                    LocalDateTime.now().plusMinutes(verificationProperties.getSignupWindowMinutes()), true));
            return true;
        }
        return false;
    }

    public boolean isEmailVerified(String email) {
        VerificationInfo info = verificationStore.find(email);
        if (info != null && info.verified() && LocalDateTime.now().isBefore(info.expiryTime())) {
            log.info("인증 코드 삭제됨 : {}", info.code());
            verificationStore.invalidate(email);
            return true;
        }
        return false;
    }

    private String generateAuthCode() {
        SecureRandom random = new SecureRandom();
        int num = random.nextInt(900000) + 100000;
        return String.valueOf(num);
    }
}
