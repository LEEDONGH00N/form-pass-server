package com.example.reservation_solution.global.security;

import com.example.reservation_solution.api.auth.domain.Host;
import com.example.reservation_solution.api.auth.repository.HostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HostUserDetailsService implements UserDetailsService {

    private final HostRepository hostRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Host host = hostRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자입니다: " + email));
        return new HostUserDetails(host);
    }
}
