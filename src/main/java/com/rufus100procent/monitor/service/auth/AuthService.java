package com.rufus100procent.monitor.service.auth;

import com.rufus100procent.monitor.dto.auth.AuthResponse;
import com.rufus100procent.monitor.dto.auth.LoginRequest;
import com.rufus100procent.monitor.dto.auth.RegisterRequest;
import com.rufus100procent.monitor.modal.MonitorUser;
import com.rufus100procent.monitor.repo.UserRepository;
import com.rufus100procent.monitor.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ReactiveAuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            ReactiveAuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public Mono<String> register(RegisterRequest request) {
        return userRepository.existsByUsername(request.getUsername())
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Registration failed: username '{}' already exists", request.getUsername());
                        return Mono.error(new IllegalArgumentException(
                                "Username '" + request.getUsername() + "' is already taken"
                        ));
                    }

                    MonitorUser user = new MonitorUser();
                    user.setId(UUID.randomUUID());
                    user.setIsNew(true);
                    user.setUsername(request.getUsername());
                    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                    user.setRole("ROLE_ADMIN");
                    user.setCreatedAt(Instant.now());

                    return userRepository.save(user)
                            .doOnNext(savedUser -> log.info(
                                    "User registered: id={}, username={}", savedUser.getId(), savedUser.getUsername()
                            ))
                            .map(savedUser -> "Successfully created user: " + savedUser.getId());
                });
    }

    public Mono<AuthResponse> login(LoginRequest request) {
        return authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()
                ))
                .onErrorMap(BadCredentialsException.class, _ -> {
                    log.warn("Login failed: bad credentials for username '{}'", request.getUsername());
                    return new IllegalArgumentException("Invalid username or password");
                })
                .then(userRepository.findByUsername(request.getUsername()))
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("User '" + request.getUsername() + "' not found")
                ))
                .flatMap(user -> {
                    user.setIsNew(false);
                    user.setLastLoginAt(Instant.now());
                    return userRepository.save(user)
                            .map(savedUser -> {
                                assert savedUser.getId() != null;
                                String token = jwtUtil.generateToken(
                                        savedUser.getId(),
                                        savedUser.getUsername(),
                                        savedUser.getRole(),
                                        savedUser.getCreatedAt()
                                );
                                log.info("User logged in: id={}, username={}", savedUser.getId(), savedUser.getUsername());
                                return new AuthResponse(token);
                            });
                });
    }
}