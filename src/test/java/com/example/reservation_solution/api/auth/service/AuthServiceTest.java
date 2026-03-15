package com.example.reservation_solution.api.auth.service;

import com.example.reservation_solution.api.auth.domain.Host;
import com.example.reservation_solution.api.auth.dto.LoginRequest;
import com.example.reservation_solution.api.auth.dto.SignupRequest;
import com.example.reservation_solution.api.auth.dto.TokenResponse;
import com.example.reservation_solution.api.auth.repository.HostRepository;
import com.example.reservation_solution.global.security.JwtProvider;
import com.example.reservation_solution.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private HostRepository hostRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        // given
        SignupRequest request = new SignupRequest("new@example.com", "password123", "새사용자");

        given(hostRepository.existsByEmail("new@example.com")).willReturn(false);
        given(emailVerificationService.isEmailVerified("new@example.com")).willReturn(true);
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");

        // when
        authService.signup(request);

        // then
        verify(hostRepository).save(any(Host.class));
    }

    @Test
    @DisplayName("이메일 중복 시 예외가 발생한다")
    void signup_shouldThrowException_whenEmailDuplicate() {
        // given
        SignupRequest request = new SignupRequest("dup@example.com", "password123", "중복사용자");
        given(hostRepository.existsByEmail("dup@example.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 이메일입니다");
    }

    @Test
    @DisplayName("이메일 미인증 시 예외가 발생한다")
    void signup_shouldThrowException_whenEmailNotVerified() {
        // given
        SignupRequest request = new SignupRequest("unverified@example.com", "password123", "미인증사용자");
        given(hostRepository.existsByEmail("unverified@example.com")).willReturn(false);
        given(emailVerificationService.isEmailVerified("unverified@example.com")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일 인증이 완료되지 않았습니다. 인증을 먼저 진행해주세요.");
    }

    @Test
    @DisplayName("로그인 성공 시 토큰을 반환한다")
    void login_success() {
        // given
        Host host = TestFixtures.createHost();
        LoginRequest request = new LoginRequest("host@example.com", "password123");

        given(hostRepository.findByEmail("host@example.com")).willReturn(Optional.of(host));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(jwtProvider.generateToken(anyString(), anyString())).willReturn("jwt-token");

        // when
        TokenResponse response = authService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("jwt-token");
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 로그인 시 예외가 발생한다")
    void login_shouldThrowException_whenUserNotFound() {
        // given
        LoginRequest request = new LoginRequest("unknown@example.com", "password123");
        given(hostRepository.findByEmail("unknown@example.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 사용자입니다");
    }

    @Test
    @DisplayName("비밀번호 불일치 시 예외가 발생한다")
    void login_shouldThrowException_whenPasswordNotMatch() {
        // given
        Host host = TestFixtures.createHost();
        LoginRequest request = new LoginRequest("host@example.com", "wrongPassword");

        given(hostRepository.findByEmail("host@example.com")).willReturn(Optional.of(host));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 일치하지 않습니다");
    }
}
