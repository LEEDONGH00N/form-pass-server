package com.example.reservation_solution.api.auth.controller;

import com.example.reservation_solution.api.auth.dto.EmailSendRequest;
import com.example.reservation_solution.api.auth.dto.EmailVerifyRequest;
import com.example.reservation_solution.global.docs.SendEmailAuthDocs;
import com.example.reservation_solution.global.docs.VerifyEmailAuthDocs;
import com.example.reservation_solution.api.auth.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
@Tag(name = "Email Auth", description = "이메일 인증 API")
public class EmailAuthController {

    private final EmailVerificationService verificationService;

    @SendEmailAuthDocs
    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody EmailSendRequest request) {
        verificationService.sendCode(request.email());
        return ResponseEntity.ok("인증 메일이 발송되었습니다.");
    }

    @VerifyEmailAuthDocs
    @PostMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestBody EmailVerifyRequest request) {
        boolean isVerified = verificationService.verifyCode(request.email(), request.authCode());
        if (isVerified) {
            return ResponseEntity.ok("이메일 인증에 성공했습니다.");
        } else {
            return ResponseEntity.badRequest().body("인증 코드가 올바르지 않거나 만료되었습니다.");
        }
    }
}