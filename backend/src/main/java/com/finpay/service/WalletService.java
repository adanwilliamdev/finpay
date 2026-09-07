package com.finpay.service;

import com.finpay.dto.response.WalletResponse;
import com.finpay.entity.User;
import com.finpay.entity.Wallet;
import com.finpay.exception.UserNotFoundException;
import com.finpay.exception.WalletNotFoundException;
import com.finpay.repository.UserRepository;
import com.finpay.repository.WalletRepository;
import com.finpay.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public WalletResponse getWalletById(String walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        String ownerId = wallet.getUser() != null ? wallet.getUser().getId() : null;
        SecurityUtil.checkOwnerOrAdmin(ownerId);

        return convertToResponse(wallet);
    }

    public WalletResponse getWalletByUserId(String userId) {
        // Only the owner themselves (or an admin) can look up a wallet by user id
        SecurityUtil.checkOwnerOrAdmin(userId);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        return convertToResponse(wallet);
    }

    public List<WalletResponse> getAllWallets() {
        return walletRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WalletResponse blockWallet(String walletId) {
        log.info("Blocking wallet: {}", walletId);
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        if (wallet.getStatus() == Wallet.WalletStatus.BLOCKED) {
            throw new IllegalStateException("Wallet is already blocked");
        }

        wallet.setStatus(Wallet.WalletStatus.BLOCKED);
        wallet = walletRepository.save(wallet);

        auditService.logAction("BLOCK_WALLET", "Wallet", walletId,
                "ACTIVE", "BLOCKED", "Wallet blocked");

        return convertToResponse(wallet);
    }

    @Transactional
    public WalletResponse activateWallet(String walletId) {
        log.info("Activating wallet: {}", walletId);
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        if (wallet.getStatus() == Wallet.WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is already active");
        }

        wallet.setStatus(Wallet.WalletStatus.ACTIVE);
        wallet = walletRepository.save(wallet);

        auditService.logAction("ACTIVATE_WALLET", "Wallet", walletId,
                "BLOCKED", "ACTIVE", "Wallet activated");

        return convertToResponse(wallet);
    }

    @Transactional
    public void updateBalance(String walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            wallet.credit(amount);
        } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
            wallet.debit(amount.abs());
        }

        walletRepository.save(wallet);
    }

    private WalletResponse convertToResponse(Wallet wallet) {
        User user = wallet.getUser();
        return WalletResponse.builder()
                .id(wallet.getId())
                .walletNumber(wallet.getWalletNumber())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus().name())
                .userId(user != null ? user.getId() : null)
                .userFullName(user != null ? user.getFullName() : null)
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}