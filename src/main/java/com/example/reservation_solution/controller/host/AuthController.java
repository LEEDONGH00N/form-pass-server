package com.example.reservation_solution.controller.host;

import com.example.reservation_solution.dto.LoginRequest;
import com.example.reservation_solution.dto.LoginResponse;
import com.example.reservation_solution.dto.SignupRequest;
import com.example.reservation_solution.global.docs.LoginDocs;
import com.example.reservation_solution.global.docs.LogoutDocs;
import com.example.reservation_solution.global.docs.SignupDocs;
import com.example.reservation_solution.global.security.CookieUtils;
import com.example.reservation_solution.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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
        ResponseCookie cookie = CookieUtils.createAccessTokenCookie(token);
        LoginResponse response = LoginResponse.success(request.email());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @LogoutDocs
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = CookieUtils.deleteAccessTokenCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}
