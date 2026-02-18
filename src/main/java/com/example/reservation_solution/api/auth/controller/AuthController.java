package com.example.reservation_solution.api.auth.controller;

import com.example.reservation_solution.api.auth.dto.LoginRequest;
import com.example.reservation_solution.api.auth.dto.LoginResponse;
import com.example.reservation_solution.api.auth.dto.SignupRequest;
import com.example.reservation_solution.global.docs.LoginDocs;
import com.example.reservation_solution.global.docs.SignupDocs;
import com.example.reservation_solution.api.auth.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 관련 API (회원가입, 로그인)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @SignupDocs
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @LoginDocs
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.generateLoginToken(request);
        return ResponseEntity.ok(LoginResponse.of(token, request.email()));
    }
}
