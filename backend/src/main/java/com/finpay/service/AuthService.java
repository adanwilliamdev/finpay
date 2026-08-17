package com.finpay.service;

import com.finpay.dto.request.LoginRequest;
import com.finpay.dto.request.RegisterRequest;
import com.finpay.dto.response.AuthResponse;
import com.finpay.entity.User;
import com.finpay.entity.Wallet;
import com.finpay.exception.UserNotFoundException;
import com.finpay.repository.UserRepository;
import com.finpay.repository.WalletRepository;
import com.finpay.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (request.getDocumentNumber() != null &&
                userRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new IllegalArgumentException("Document number already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .documentNumber(request.getDocumentNumber())
                .role(User.UserRole.USER)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        user = userRepository.save(user);

        // Create wallet for user
        Wallet wallet = Wallet.builder()
                .walletNumber(generateWalletNumber())
                .balance(BigDecimal.ZERO)
                .currency("BRL")
                .status(Wallet.WalletStatus.ACTIVE)
                .user(user)
                .build();

        wallet = walletRepository.save(wallet);
        user.setWallet(wallet);
        userRepository.save(user);

        log.info("User registered successfully with ID: {}", user.getId());

        // Generate token
        String token = tokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("User account is disabled");
        }

        if (!user.isAccountNonLocked()) {
            throw new IllegalArgumentException("User account is locked");
        }

        String token = tokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    public void logout(String token) {
        log.info("Logout attempt");
        // In JWT, we would typically invalidate the token
        // For simplicity, we'll just log the action
        auditService.logAction("LOGOUT", "User", null, null, null, "User logged out");
    }

    private String generateWalletNumber() {
        return "WP" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}