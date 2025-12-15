package com.example.reservation_solution.controller.host;

import com.example.reservation_solution.dto.EmailSendRequest;
import com.example.reservation_solution.dto.EmailVerifyRequest;
import com.example.reservation_solution.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
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

    @PostMapping("/send")
    @Operation(summary = "인증 메일 전송", description = "입력한 이메일로 6자리 인증 코드를 전송합니다.")
    public ResponseEntity<String> sendEmail(@RequestBody EmailSendRequest request) {
        verificationService.sendCode(request.getEmail());
        return ResponseEntity.ok("인증 메일이 발송되었습니다.");
    }

    @PostMapping("/verify")
    @Operation(summary = "인증 코드 검증", description = "이메일과 인증 코드를 입력받아 유효성을 검사합니다.")
    public ResponseEntity<String> verifyEmail(@RequestBody EmailVerifyRequest request) {
        boolean isVerified = verificationService.verifyCode(request.getEmail(), request.getAuthCode());
        if (isVerified) {
            return ResponseEntity.ok("이메일 인증에 성공했습니다.");
        } else {
            return ResponseEntity.badRequest().body("인증 코드가 올바르지 않거나 만료되었습니다.");
        }
    }
}