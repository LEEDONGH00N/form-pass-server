package com.example.reservation_solution.service;

import com.example.reservation_solution.domain.Host;
import com.example.reservation_solution.domain.Role;
import com.example.reservation_solution.dto.LoginRequest;
import com.example.reservation_solution.dto.SignupRequest;
import com.example.reservation_solution.dto.TokenResponse;
import com.example.reservation_solution.repository.HostRepository;
import com.example.reservation_solution.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final HostRepository hostRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public void signup(SignupRequest request) {
        validateExistEmail(request);
        checkEmailVerified(request);
        Host host = Host.create(request.email(), passwordEncoder.encode(request.password()), request.name(), Role.HOST);
        hostRepository.save(host);
    }

    public TokenResponse login(LoginRequest request) {
        Host host = loadHostOrThrow(request);
        checkPassword(request, host);
        String token = jwtProvider.generateToken(host.getEmail(), host.getRole().name());
        return new TokenResponse(token);
    }

    private void checkPassword(LoginRequest request, Host host) {
        if (!passwordEncoder.matches(request.password(), host.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }
    }

    private Host loadHostOrThrow(LoginRequest request) {
        return hostRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));
    }

    private void validateExistEmail(SignupRequest request) {
        if (hostRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다");
        }
    }

    private void checkEmailVerified(SignupRequest request) {
        if (!emailVerificationService.isEmailVerified(request.email())) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다. 인증을 먼저 진행해주세요.");
        }
    }
}
