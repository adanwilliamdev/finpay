package com.finpay.controller;

import com.finpay.dto.response.WalletResponse;
import com.finpay.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WalletResponse>> getAllWallets() {
        return ResponseEntity.ok(walletService.getAllWallets());
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletResponse> getWalletById(@PathVariable String walletId) {
        return ResponseEntity.ok(walletService.getWalletById(walletId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<WalletResponse> getWalletByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(walletService.getWalletByUserId(userId));
    }

    @PostMapping("/{walletId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletResponse> blockWallet(@PathVariable String walletId) {
        return ResponseEntity.ok(walletService.blockWallet(walletId));
    }

    @PostMapping("/{walletId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletResponse> activateWallet(@PathVariable String walletId) {
        return ResponseEntity.ok(walletService.activateWallet(walletId));
    }
}