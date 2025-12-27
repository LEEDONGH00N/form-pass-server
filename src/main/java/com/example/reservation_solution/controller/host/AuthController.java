package com.example.reservation_solution.controller.host;

import com.example.reservation_solution.dto.LoginRequest;
import com.example.reservation_solution.dto.LoginResponse;
import com.example.reservation_solution.dto.SignupRequest;
import com.example.reservation_solution.dto.TokenResponse;
import com.example.reservation_solution.global.security.CookieUtils;
import com.example.reservation_solution.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "회원가입", description = "호스트 계정을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이메일 중복 등)")
    })
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "로그인", description = "호스트 계정으로 로그인하여 JWT 토큰을 HttpOnly 쿠키로 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공, JWT 토큰이 HttpOnly 쿠키로 설정됨"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (이메일 또는 비밀번호 오류)")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.generateLoginToken(request);
        ResponseCookie cookie = CookieUtils.createAccessTokenCookie(token);
        LoginResponse response = LoginResponse.success(request.email());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @Operation(summary = "로그아웃", description = "현재 세션의 JWT 토큰을 무효화하고 쿠키를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = CookieUtils.deleteAccessTokenCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}
